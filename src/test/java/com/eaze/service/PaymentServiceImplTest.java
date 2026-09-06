package com.eaze.service;

import com.eaze.domian.PaymentMethod;
import com.eaze.domian.PaymentOrderStatus;
import com.eaze.exceptions.PaymentException;
import com.eaze.model.PaymentOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eaze.repository.PaymentOrderRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.proceedPaymentOrder.
 *
 * NOTE: the RAZORPAY + PENDING + "captured" success path constructs a real
 * RazorpayClient(apiKey, apiSecret) and makes a network call, which cannot be
 * mocked without refactoring the service to inject the client. Those success/failed
 * verification cases are therefore covered by integration tests, not here.
 * These unit tests cover the branches that need no Razorpay client:
 *  - replay protection (order not PENDING)
 *  - Stripe "coming soon" guard
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentOrderRepository paymentOrderRepository;

    @InjectMocks private PaymentServiceImpl paymentService;

    @Test
    void proceedPaymentOrder_returnsFalse_whenOrderNotPending_replayProtection() throws Exception {
        PaymentOrder order = new PaymentOrder();
        order.setStatus(PaymentOrderStatus.SUCCESS); // already processed
        order.setPaymentMethod(PaymentMethod.RAZORPAY);

        Boolean result = paymentService.proceedPaymentOrder(order, "pay_123");

        assertFalse(result, "an already-processed order must not be credited again");
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void proceedPaymentOrder_returnsFalse_whenOrderFailed() throws Exception {
        PaymentOrder order = new PaymentOrder();
        order.setStatus(PaymentOrderStatus.FAILED);
        order.setPaymentMethod(PaymentMethod.RAZORPAY);

        assertFalse(paymentService.proceedPaymentOrder(order, "pay_123"));
    }

    @Test
    void proceedPaymentOrder_stripe_throwsComingSoon() {
        PaymentOrder order = new PaymentOrder();
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.STRIPE);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.proceedPaymentOrder(order, "pay_123"));
        assertTrue(ex.getMessage().toLowerCase().contains("stripe"));
    }
}
