package com.eaze.service.domain;

import com.eaze.model.User;
import com.eaze.model.Withdrawal;

import java.math.BigDecimal;
import java.util.List;

public interface WithdrawalService {

    Withdrawal requestWithdrawal(BigDecimal amount, User user);

    Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept) throws Exception;

    List<Withdrawal> getUserWithdrawalHistory(User user);

    List<Withdrawal> getAllWithdrawalRequest();
}
