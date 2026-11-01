package com.eaze.service;

import com.eaze.domian.OrderStatus;
import com.eaze.domian.OrderType;
import com.eaze.domian.WalletTransactionType;
import com.eaze.model.Order;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.repository.OrderRepository;
import com.eaze.repository.WalletRepository;
import com.eaze.service.domain.WalletService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final OrderRepository orderRepository;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionService transactionService, OrderRepository orderRepository) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.orderRepository = orderRepository;
    }

    @Override
    public Wallet getUserWallet(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet != null) {
            return wallet;
        }
        try {
            Wallet newWallet = new Wallet();
            newWallet.setUser(user);
            // saveAndFlush forces the INSERT (and any unique-constraint violation)
            // to surface here so we can handle a concurrent creation gracefully.
            return walletRepository.saveAndFlush(newWallet);
        } catch (DataIntegrityViolationException e) {
            // Another request created the wallet first (unique constraint on user_id).
            // Return the existing wallet instead of failing.
            Wallet existing = walletRepository.findByUserId(user.getId());
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    @Override
    public Wallet addBalance(Wallet wallet, BigDecimal money) {
        BigDecimal balance = wallet.getBalance();
        BigDecimal newBalance = balance.add(money);

        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet deposit(Wallet wallet, BigDecimal amount) {
        Wallet updated = addBalance(wallet, amount);

        // Ledger: record the deposit as a credit to the wallet.
        transactionService.createTransaction(
                updated,
                WalletTransactionType.ADD_MONEY,
                LocalDateTime.now(),
                null,
                "wallet top-up",
                amount);

        return updated;
    }

    @Override
    public Wallet findWalletById(Long id) throws Exception {
        Optional<Wallet> wallet = walletRepository.findById(id);
        if (wallet.isPresent()){
            return wallet.get();
        }
        throw new Exception("wallet not found!!!");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, BigDecimal amount) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <=0 ) {
            throw new Exception("Transfer amount must be greater than 0");
        }
        Wallet senderWalletUnlocked = walletRepository.findByUserId(sender.getId());
        if (senderWalletUnlocked == null) {
            throw new Exception("Sender wallet not found");
        }
        Long senderWalletId = senderWalletUnlocked.getId();
        Long receiverWalletId = receiverWallet.getId();

        if (senderWalletId.equals(receiverWalletId)) {
            throw new Exception("Cannot transfer to your own wallet");
        }
        // DEADLOCK AVOIDANCE: always acquire the two row locks in a globally consistent
        // order (ascending wallet id), regardless of who is sender/receiver. This removes
        // the circular-wait condition, so two opposite-direction transfers can never deadlock.
        Long firstId = Math.min(senderWalletId, receiverWalletId);
        Long secondId = Math.max(senderWalletId, receiverWalletId);

        Wallet firstLocked = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new Exception("Wallet not found: " + firstId));
        Wallet secondLocked = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new Exception("Wallet not found: " + secondId));
        Wallet senderWallet = senderWalletId.equals(firstId) ? firstLocked : secondLocked;
        Wallet receiver = receiverWalletId.equals(firstId) ? firstLocked : secondLocked;
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance");
        }
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(receiver);

        // Ledger: two correlated records (sender debit + receiver credit).
        String transferId = UUID.randomUUID().toString();
        transactionService.createTransaction(
                senderWallet,
                WalletTransactionType.WALLET_TRANSFER,
                LocalDateTime.now(),
                transferId,
                "Transfer to wallet id " + receiver.getId(),
                amount);
        transactionService.createTransaction(
                receiver,
                WalletTransactionType.WALLET_TRANSFER,
                LocalDateTime.now(),
                transferId,
                "Transfer from wallet id " + senderWallet.getId(),
                amount);

        return senderWallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet payOrderPayment(Order order, User user) throws Exception {
        // Only a PENDING order may be paid. Blocks double-charge / double-credit.
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new Exception("Order " + order.getId() + " is already "
                    + order.getOrderStatus() + " and cannot be paid again");
        }
        Wallet wallet = getUserWalletForUpdate(user);
        BigDecimal newBalance;
        WalletTransactionType ledgerType;
        String purpose;
        if (order.getOrderType().equals(OrderType.BUY)) {
            newBalance = wallet.getBalance().subtract(order.getPrice());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new Exception("Insufficient funds for this transaction");
            }
            ledgerType = WalletTransactionType.BUY_ASSET;
            purpose = "Order payment (BUY) #"+order.getId();
        } else {
            newBalance = wallet.getBalance().add(order.getPrice());
            ledgerType = WalletTransactionType.SELL_ASSET;
            purpose = "Order Payment(SELL) #"+order.getId();
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);

        transactionService.createTransaction(
                wallet,
                ledgerType,
                LocalDateTime.now(),
                null,
                purpose,
                order.getPrice());
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet getUserWalletForUpdate(User user) throws Exception {
        return walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new Exception("Wallet not found for user " + user.getId()));
    }
}
