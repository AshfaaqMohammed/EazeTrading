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

}
