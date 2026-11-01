package com.eaze.service.domain;

import com.eaze.model.Order;
import com.eaze.model.User;
import com.eaze.model.Wallet;

import java.math.BigDecimal;

public interface WalletService {

    Wallet getUserWallet(User user);

    Wallet addBalance(Wallet wallet, BigDecimal money);

    Wallet deposit(Wallet wallet, BigDecimal amount);

    Wallet findWalletById(Long id) throws Exception;

    Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, BigDecimal amount) throws Exception;

    Wallet payOrderPayment(Order order, User user) throws Exception;

    /**
     * Fetch the user's wallet with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
     * MUST be called inside a transaction; the lock is held until commit. Ensures
     * the wallet row exists first, then locks it. Use on all debit paths.
     */
    Wallet getUserWalletForUpdate(User user) throws Exception;

}
