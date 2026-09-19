package com.eaze.controller;

import com.eaze.domian.WalletTransactionType;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.model.WalletTransaction;
import com.eaze.model.Withdrawal;
import com.eaze.service.TransactionService;
import com.eaze.service.domain.UserService;
import com.eaze.service.domain.WalletService;
import com.eaze.service.domain.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WalletService walletService;
    private final UserService userService;
    private final TransactionService transactionService;

    @PostMapping("/api/withdrawal/{amount}")
    public ResponseEntity<?> withdrawalRequest(@PathVariable("amount") BigDecimal amount,
                                              @RequestHeader("Authorization") String jwt) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        Withdrawal withdrawal = withdrawalService.createWithdrawalRequest(amount, user);
        return new ResponseEntity<>(withdrawal, HttpStatus.OK);
    }

    @PatchMapping("/api/admin/withdrawal/{id}/proceed/{accept}")
    public ResponseEntity<?> proceedWithdrawal(@PathVariable("id") Long id,
                                               @PathVariable("accept") boolean accept,
                                               @RequestHeader("Authorization") String jwt) throws Exception {

        Withdrawal withdrawal = withdrawalService.proceedWithdrawal(id, accept);
        return new ResponseEntity<>(withdrawal, HttpStatus.OK);
    }

    @GetMapping("/api/withdrawal")
    public ResponseEntity<List<Withdrawal>> getWithdrawalHistory(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        List<Withdrawal> withdrawalList = withdrawalService.getUserWithdrawalHistory(user);
        return new ResponseEntity<>(withdrawalList, HttpStatus.OK);
    }

    @GetMapping("/api/admin/withdrawal")
    public ResponseEntity<List<Withdrawal>> getAllWithdrawalRequest(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        List<Withdrawal> withdrawalList = withdrawalService.getAllWithdrawalRequest();
        return new ResponseEntity<>(withdrawalList, HttpStatus.OK);
    }
}
