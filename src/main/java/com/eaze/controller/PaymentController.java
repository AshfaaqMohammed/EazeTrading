package com.eaze.controller;

import com.eaze.domian.PaymentMethod;
import com.eaze.model.PaymentOrder;
import com.eaze.model.User;
import com.eaze.response.PaymentResponse;
import com.eaze.service.domain.PaymentService;
import com.eaze.service.domain.UserService;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PaymentController {

    private final UserService userService;
    private final PaymentService paymentService;

    @PostMapping("/payment/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse> paymentHandler(@RequestHeader("Authorization") String jwt,
                                                          @PathVariable PaymentMethod paymentMethod,
                                                          @PathVariable BigDecimal amount) throws Exception, RazorpayException, StripeException {
        User user = userService.findUserProfileByJwt(jwt);

        PaymentOrder order = paymentService.createOrder(user, amount, paymentMethod);

        PaymentResponse paymentResponse;

        if (paymentMethod.equals(PaymentMethod.RAZORPAY)) {
            paymentResponse = paymentService.createRazorPaymentLink(user, amount, order.getId());
        }else{
            paymentResponse = paymentService.createStripePaymentLink(user, amount, order.getId());
        }

        return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
    }
}
