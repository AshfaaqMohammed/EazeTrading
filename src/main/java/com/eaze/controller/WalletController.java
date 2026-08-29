package com.eaze.controller;

import com.eaze.domian.PaymentOrderStatus;
import com.eaze.exceptions.PaymentException;
import com.eaze.model.*;
import com.eaze.service.domain.OrderService;
import com.eaze.service.domain.PaymentService;
import com.eaze.service.domain.UserService;
import com.eaze.service.domain.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<Wallet> getUserWallet(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);
        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/{walletId}/transfer")
    public ResponseEntity<Wallet> walletToWalletTransfer(
            @RequestHeader("Authorization") String jwt,
            @PathVariable("walletId") Long walletId,
            @RequestBody WalletTransaction req) throws Exception {

        User senderUser = userService.findUserProfileByJwt(jwt);
        Wallet receiverWallet = walletService.findWalletById(walletId);
        Wallet wallet = walletService.walletToWalletTransfer(senderUser, receiverWallet, req.getAmount());

        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/order/{orderId}/pay")
    public ResponseEntity<Wallet> payOrderPayment(
            @RequestHeader("Authorization") String jwt,
            @PathVariable("orderId") Long orderId) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Order order = orderService.getOrderById(orderId);
        Wallet wallet = walletService.payOrderPayment(order, user);

        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/deposit")
    public ResponseEntity<Wallet> addMoneyToWallet(@RequestHeader("Authorization") String jwt,
                                                  @RequestParam(name = "order_id") Long orderId,
                                                  @RequestParam(name = "payment_id") String paymentId) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);
        PaymentOrder order = paymentService.getPaymentOrderById(orderId);

        if (!order.getStatus().equals(PaymentOrderStatus.PENDING)) {
            throw new PaymentException("This payment order has already been processed.", HttpStatus.CONFLICT);
        }

        Boolean status = paymentService.proceedPaymentOrder(order, paymentId);

        if (status) {
            wallet = walletService.addBalance(wallet, order.getAmount());
        }else {
            throw new PaymentException("Payment verification failed. No funds were added.", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }
}
