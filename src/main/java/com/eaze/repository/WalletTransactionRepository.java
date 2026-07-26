package com.eaze.repository;

import com.eaze.model.Wallet;
import com.eaze.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> getWalletTransactionsByWalletId(Long id);
}
