package com.eaze.service.domain;

import com.eaze.domian.PaymentMethod;
import com.eaze.model.PaymentOrder;
import com.eaze.model.User;
import com.eaze.response.PaymentResponse;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentOrder createOrder(User user, BigDecimal amount, PaymentMethod paymentMethod);

    PaymentOrder getPaymentOrderById(Long id) throws Exception;

    Boolean proceedPaymentOrder(PaymentOrder paymentOrder, String paymentId) throws RazorpayException;

    PaymentResponse createRazorPaymentLink(User user, BigDecimal amount, Long userId);

    PaymentResponse createStripePaymentLink(User user, BigDecimal amount, Long orderId) throws StripeException;
}
