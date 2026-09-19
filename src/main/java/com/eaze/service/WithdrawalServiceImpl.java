package com.eaze.service;

import com.eaze.domian.WalletTransactionType;
import com.eaze.domian.WithdrawalStatus;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.model.Withdrawal;
import com.eaze.repository.WithdrawalRepository;
import com.eaze.service.domain.WalletService;
import com.eaze.service.domain.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal createWithdrawalRequest(BigDecimal amount, User user) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Withdrawal amount must be greater than 0");
        }
        Wallet userWallet = walletService.getUserWallet(user);
        if (userWallet.getBalance().compareTo(amount) < 0) {
            throw new Exception("Insufficient balance for withdrawal");
        }

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUser(user);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        Withdrawal saved = withdrawalRepository.save(withdrawal);

        walletService.addBalance(userWallet,saved.getAmount().negate());

        transactionService.createTransaction(
                userWallet,
                WalletTransactionType.WITHDRAWAL,
                LocalDateTime.now(),
                null,
                "Bank account withdrawal",
                saved.getAmount());
        return saved;
    }

    @Override
    public Withdrawal requestWithdrawal(BigDecimal amount, User user) {

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUser(user);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept) throws Exception {

        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);

        if (withdrawal.isEmpty()) {
            throw new Exception("Withdrawal not found!!");
        }
        Withdrawal legitWithdrawal = withdrawal.get();
        if (legitWithdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new Exception("Withdrawal " + withdrawalId + " is already " + legitWithdrawal.getStatus()
                    + " and cannot be processed again");
        }
        legitWithdrawal.setDate(LocalDateTime.now());
        if (accept) {
            legitWithdrawal.setStatus(WithdrawalStatus.SUCCESS);
        }else{
            legitWithdrawal.setStatus(WithdrawalStatus.DECLINE);
            Wallet ownerWallet = walletService.getUserWallet(legitWithdrawal.getUser());
            walletService.addBalance(ownerWallet, legitWithdrawal.getAmount());
        }

        return withdrawalRepository.save(legitWithdrawal);
    }

    @Override
    public List<Withdrawal> getUserWithdrawalHistory(User user) {
        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAll();
    }
}
