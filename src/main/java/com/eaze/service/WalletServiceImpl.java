package com.eaze.service;

import com.eaze.domian.OrderType;
import com.eaze.domian.WalletTransactionType;
import com.eaze.model.Order;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.repository.WalletRepository;
import com.eaze.service.domain.WalletService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
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
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, BigDecimal amount) throws Exception {
        Wallet senderWallet = walletRepository.findByUserId(sender.getId());

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance");
        }
        BigDecimal senderBalance = senderWallet.getBalance().subtract(amount);
        senderWallet.setBalance(senderBalance);
        walletRepository.save(senderWallet);

        addBalance(receiverWallet, amount);

        // Ledger: two correlated records (sender debit + receiver credit).
        String transferId = UUID.randomUUID().toString();
        transactionService.createTransaction(
                senderWallet,
                WalletTransactionType.WALLET_TRANSFER,
                LocalDateTime.now(),
                transferId,
                "Transfer to wallet id " + receiverWallet.getId(),
                amount);
        transactionService.createTransaction(
                receiverWallet,
                WalletTransactionType.WALLET_TRANSFER,
                LocalDateTime.now(),
                transferId,
                "Transfer from wallet id " + senderWallet.getId(),
                amount);

        return senderWallet;
    }

    @Override
    public Wallet payOrderPayment(Order order, User user) throws Exception {
        Wallet wallet = getUserWallet(user);

        BigDecimal newBalance;
        if (order.getOrderType().equals(OrderType.BUY)) {
            newBalance = wallet.getBalance().subtract(order.getPrice());
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new Exception("Insufficient funds for this transaction");
            }
        }else{
            newBalance = wallet.getBalance().add(order.getPrice());
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        return wallet;
    }
}
