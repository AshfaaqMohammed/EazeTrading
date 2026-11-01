package com.eaze.repository;

import com.eaze.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Plain read, NO lock. For read-only paths (e.g. GET /wallet) that must not
    // serialize on the row.
    Wallet findByUserId(Long userId);

    /**
     * Pessimistic (SELECT ... FOR UPDATE) fetch by user id. Must be called INSIDE
     * a @Transactional method; the exclusive row lock is held until that
     * transaction commits/rolls back, serializing concurrent debits to this wallet.
     * Used by buy, withdrawal, and the transfer-sender path.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(Long userId);

    /**
     * Pessimistic fetch by wallet id. Used to lock wallets in a consistent order
     * (ascending id) during a transfer, which prevents deadlock between two
     * transfers running in opposite directions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(Long id);
}
