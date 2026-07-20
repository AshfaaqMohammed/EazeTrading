package com.eaze.service;

import com.eaze.domian.WithdrawalStatus;
import com.eaze.model.User;
import com.eaze.model.Withdrawal;
import com.eaze.repository.WithdrawalRepository;
import com.eaze.service.domain.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;

    @Override
    public Withdrawal requestWithdrawal(Long amount, User user) {

        Withdrawal withdrawal = new Withdrawal();

        withdrawal.setUser(user);
        withdrawal.setAmount(amount);
        withdrawal.setStatus(WithdrawalStatus.PENDING);

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept) throws Exception {

        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);

        if (withdrawal.isEmpty()) {
            throw new Exception("Withdrawal not found!!");
        }
        Withdrawal legitWithdrawal = withdrawal.get();
        legitWithdrawal.setDate(LocalDateTime.now());
        if (accept) {
            legitWithdrawal.setStatus(WithdrawalStatus.SUCCESS);
        }else{
            legitWithdrawal.setStatus(WithdrawalStatus.DECLINE);
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
