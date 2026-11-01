package com.eaze.service;

import com.eaze.domian.OrderType;
import com.eaze.domian.USER_ROLE;
import com.eaze.model.Coin;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.repository.CoinRepository;
import com.eaze.repository.UserRepository;
import com.eaze.repository.WalletRepository;
import com.eaze.service.domain.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test (real MySQL via Testcontainers) proving the Task 3 concurrency
 * guarantee: a wallet funded for exactly ONE purchase, hit by many simultaneous
 * buys, allows exactly one success and never goes negative.
 *
 * This is the test that FAILS without the pessimistic wallet lock (multiple buys
 * would succeed and drive the balance negative) and PASSES with it.
 *
 * Suffix "IT" = integration test. Not @Transactional on purpose: each thread must
 * run its own independent transaction, exactly like separate HTTP requests.
 */
@SpringBootTest
public class OrderConcurrencyIT {
    @Autowired private com.eaze.repository.WalletTransactionRepository walletTransactionRepository;
    @Autowired private com.eaze.repository.OrderItemRepository orderItemRepository;
    @Autowired private com.eaze.repository.OrderRepository orderRepository;
    @Autowired private com.eaze.repository.AssetRepository assetRepository;
    @Autowired private com.eaze.repository.CoinRepository coinRepository;
    @Autowired private com.eaze.repository.UserRepository userRepository;
    @Autowired private OrderService orderService;
    @Autowired private com.eaze.repository.WalletRepository walletRepository;

    private User user;
    private Coin coin;

    private static final int CONCURRENT_BUYS = 10;
    private static final double COIN_PRICE = 100.0;   // price per unit
    private static final BigDecimal BUY_QTY = BigDecimal.ONE;      // buy 1 unit -> cost 100
    private static final BigDecimal WALLET_FUNDED = new BigDecimal("100.00");
    AtomicReference<Exception> firstError = new AtomicReference<>();// affords exactly ONE buy

    @BeforeEach
    void setUp() {
        // Clean slate for repeatable runs.
        walletTransactionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        assetRepository.deleteAll();
        walletRepository.deleteAll();
        coinRepository.deleteAll();
        userRepository.deleteAll();

        // Seed a user.
        user = new User();
        user.setEmail("race@eaze.com");
        user.setFullName("Race Tester");
        user.setPassword("x");
        user.setRole(USER_ROLE.ROLE_CUSTOMER);
        user = userRepository.save(user);

        // Seed that user's wallet funded for exactly one purchase.
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(WALLET_FUNDED);
        walletRepository.save(wallet);

        // Seed a coin with a known price (buy path reads price off the entity, no network).
        coin = new Coin();
        coin.setId("race-coin");
        coin.setName("Race Coin");
        coin.setSymbol("RC");
        coin.setCurrentPrice(COIN_PRICE);
        coin = coinRepository.save(coin);
    }

    @Test
    void only_one_of_many_concurrent_buys_succeeds_and_balance_never_negative() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_BUYS);
        CountDownLatch startGate = new CountDownLatch(1);   // released once to fire all threads together
        CountDownLatch doneGate = new CountDownLatch(CONCURRENT_BUYS);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_BUYS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();   // all threads wait here...
                    orderService.processOrder(coin, BUY_QTY, OrderType.BUY, user);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    // Insufficient funds (or exhausted retries) -> expected for the losers.
                    failures.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();                       // ...GO: all fire at once
        doneGate.await(30, TimeUnit.SECONDS);        // wait for all to finish
        pool.shutdownNow();

        if (firstError.get() != null) {
            System.out.println(">>> First failure cause:");
            firstError.get().printStackTrace();
        }
        // --- Invariants ---
        assertEquals(1, successes.get(),
                "exactly one concurrent buy should succeed when the wallet affords only one");
        assertEquals(CONCURRENT_BUYS - 1, failures.get(),
                "all other concurrent buys should fail");

        Wallet finalWallet = walletRepository.findByUserId(user.getId());
        // One successful buy of 100 from a 100 balance -> 0.00, and NEVER negative.
        assertEquals(0, finalWallet.getBalance().compareTo(new BigDecimal("0.00")),
                "final balance should be exactly 0 after one successful buy");
        assertTrue(finalWallet.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "balance must never go negative");
    }
}
