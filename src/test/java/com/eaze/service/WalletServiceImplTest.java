package com.eaze.service;

import com.eaze.domian.OrderType;
import com.eaze.domian.WalletTransactionType;
import com.eaze.model.Order;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@eaze.com");
    }

    private Wallet walletWith(Long id, Long userId, BigDecimal balance) {
        Wallet w = new Wallet();
        w.setId(id);
        User u = new User();
        u.setId(userId);
        w.setUser(u);
        w.setBalance(balance);
        return w;
    }

    // ---------- getUserWallet ----------

    @Test
    void getUserWallet_returnsExisting_whenFound() {
        Wallet existing = walletWith(10L, 1L, new BigDecimal("500"));
        when(walletRepository.findByUserId(1L)).thenReturn(existing);

        Wallet result = walletService.getUserWallet(user);

        assertSame(existing, result);
        verify(walletRepository, never()).save(any());
        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void getUserWallet_createsWallet_whenAbsent() {
        when(walletRepository.findByUserId(1L)).thenReturn(null);
        when(walletRepository.saveAndFlush(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.getUserWallet(user);

        assertNotNull(result);
        assertSame(user, result.getUser());
        verify(walletRepository).saveAndFlush(any(Wallet.class));
    }

    @Test
    void getUserWallet_raceSafe_reFetchesOnDuplicateInsert() {
        Wallet existing = walletWith(10L, 1L, BigDecimal.ZERO);
        // First lookup: null (no wallet yet). Save fails (another request created it).
        // Second lookup (in catch): returns the now-existing wallet.
        when(walletRepository.findByUserId(1L)).thenReturn(null).thenReturn(existing);
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Wallet result = walletService.getUserWallet(user);

        assertSame(existing, result);
        verify(walletRepository, times(2)).findByUserId(1L);
    }

    // ---------- walletToWalletTransfer ----------

    @Test
    void transfer_movesFunds_andWritesTwoLedgerRows() throws Exception {
        Wallet sender = walletWith(1L, 1L, new BigDecimal("1000"));
        Wallet receiver = walletWith(2L, 2L, new BigDecimal("100"));
        when(walletRepository.findByUserId(1L)).thenReturn(sender);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.walletToWalletTransfer(user, receiver, new BigDecimal("300"));

        assertEquals(0, new BigDecimal("700").compareTo(sender.getBalance()));
        assertEquals(0, new BigDecimal("400").compareTo(receiver.getBalance()));

        // Two ledger rows, both WALLET_TRANSFER, correlated by a shared transferId.
        ArgumentCaptor<String> transferId = ArgumentCaptor.forClass(String.class);
        verify(transactionService, times(2)).createTransaction(
                any(Wallet.class), eq(WalletTransactionType.WALLET_TRANSFER), any(),
                transferId.capture(), any(), eq(new BigDecimal("300")));
        assertEquals(transferId.getAllValues().get(0), transferId.getAllValues().get(1),
                "both transfer rows must share the same transferId");
    }

    @Test
    void transfer_rejects_whenInsufficientBalance() {
        Wallet sender = walletWith(1L, 1L, new BigDecimal("100"));
        Wallet receiver = walletWith(2L, 2L, BigDecimal.ZERO);
        when(walletRepository.findByUserId(1L)).thenReturn(sender);

        Exception ex = assertThrows(Exception.class,
                () -> walletService.walletToWalletTransfer(user, receiver, new BigDecimal("500")));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));

        verify(walletRepository, never()).save(any());
        verify(transactionService, never()).createTransaction(any(), any(), any(), any(), any(), any());
    }

    // ---------- payOrderPayment ----------

    @Test
    void payOrderPayment_buy_subtractsPrice() throws Exception {
        Wallet wallet = walletWith(1L, 1L, new BigDecimal("1000"));
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setOrderType(OrderType.BUY);
        order.setPrice(new BigDecimal("250"));

        Wallet result = walletService.payOrderPayment(order, user);

        assertEquals(0, new BigDecimal("750").compareTo(result.getBalance()));
    }

    @Test
    void payOrderPayment_buy_rejects_whenInsufficientFunds() {
        Wallet wallet = walletWith(1L, 1L, new BigDecimal("100"));
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);

        Order order = new Order();
        order.setOrderType(OrderType.BUY);
        order.setPrice(new BigDecimal("500"));

        assertThrows(Exception.class, () -> walletService.payOrderPayment(order, user));
    }

    @Test
    void payOrderPayment_sell_addsPrice() throws Exception {
        Wallet wallet = walletWith(1L, 1L, new BigDecimal("100"));
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = new Order();
        order.setOrderType(OrderType.SELL);
        order.setPrice(new BigDecimal("300"));

        Wallet result = walletService.payOrderPayment(order, user);

        assertEquals(0, new BigDecimal("400").compareTo(result.getBalance()));
    }

    // ---------- deposit ----------

    @Test
    void deposit_creditsBalance_andLogsAddMoney() {
        Wallet wallet = walletWith(1L, 1L, new BigDecimal("100"));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.deposit(wallet, new BigDecimal("400"));

        assertEquals(0, new BigDecimal("500").compareTo(result.getBalance()));
        verify(transactionService).createTransaction(
                eq(wallet), eq(WalletTransactionType.ADD_MONEY), any(), isNull(), any(),
                eq(new BigDecimal("400")));
    }
}
