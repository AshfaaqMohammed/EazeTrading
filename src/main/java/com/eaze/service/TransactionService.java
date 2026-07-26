package com.eaze.service;

import com.eaze.domian.WalletTransactionType;
import com.eaze.model.Wallet;
import com.eaze.model.WalletTransaction;
import com.eaze.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    public List<WalletTransaction> getTransactionByWallet(Wallet wallet) {
        return walletTransactionRepository.getWalletTransactionsByWalletId(wallet.getId());
    }

    public WalletTransaction createTransaction(Wallet userWallet, WalletTransactionType walletTransactionType,
                                               LocalDateTime dateTime, String transferId, String purpose, Long amount) {
        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.setWallet(userWallet);
        walletTransaction.setType(walletTransactionType);
        walletTransaction.setDate(dateTime);
        walletTransaction.setTransferId(transferId);
        walletTransaction.setPurpose(purpose);
        walletTransaction.setAmount(amount);

        return walletTransactionRepository.save(walletTransaction);
    }
}
