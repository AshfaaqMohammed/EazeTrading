package com.eaze.service;

import com.eaze.domian.PaymentMethod;
import com.eaze.domian.PaymentOrderStatus;
import com.eaze.model.PaymentOrder;
import com.eaze.model.User;
import com.eaze.repository.PaymentOrderRepository;
import com.eaze.response.PaymentResponse;
import com.eaze.service.domain.PaymentService;
import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;

    @Value("${stripe.api.key}")
    private String stripSecretKey;

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecretKey;

    @Override
    public PaymentOrder createOrder(User user, Long amount, PaymentMethod paymentMethod) {
        PaymentOrder paymentOrder = new PaymentOrder();

        paymentOrder.setAmount(amount);
        paymentOrder.setPaymentMethod(paymentMethod);
        paymentOrder.setUser(user);
        paymentOrder.setStatus(PaymentOrderStatus.PENDING);

        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) throws Exception {
        return paymentOrderRepository.findById(id).orElseThrow(() ->
                new Exception("payment order not found"));
    }

    @Override
    public Boolean proceedPaymentOrder(PaymentOrder paymentOrder, String paymentId) throws RazorpayException {
        if (paymentOrder.getStatus().equals(PaymentOrderStatus.PENDING)) {
            if (paymentOrder.getPaymentMethod().equals(PaymentMethod.RAZORPAY)) {
                RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecretKey);
                Payment payment = razorpay.payments.fetch(paymentId);

                Integer amount = payment.get("amount");
                String status = payment.get("status");

                if (status.equals("captured")) {
                    paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
                    return true;
                }else{
                    paymentOrder.setStatus(PaymentOrderStatus.FAILED);
                }
                paymentOrderRepository.save(paymentOrder);
            }
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
        }
        return false;
    }

    @Override
    public PaymentResponse createRazorPaymentLink(User user, Long amount, Long orderId) {
        Long Amount = amount * 100;

        try {
            RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecretKey);

            JSONObject paymentLinkRequest = getJsonObject(user, Amount,orderId);

            PaymentLink paymentLink = razorpay.paymentLink.create(paymentLinkRequest);

            String paymentLinkId = paymentLink.get("id");
            String paymentLinkUrl = paymentLink.get("short_url");

            PaymentResponse res = new PaymentResponse();
            res.setPaymentUrl(paymentLinkUrl);

            return res;
        } catch (RazorpayException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject getJsonObject(User user, Long Amount, Long orderId) {
        JSONObject paymentLinkRequest = new JSONObject();
        paymentLinkRequest.put("amount", Amount);
        paymentLinkRequest.put("currency","INR");

        JSONObject customer = new JSONObject();
        customer.put("name", user.getFullName());
        customer.put("email", user.getEmail());

        paymentLinkRequest.put("customer", customer);

        JSONObject notify = new JSONObject();
        notify.put("email", true);
        paymentLinkRequest.put("notify", notify);

        paymentLinkRequest.put("reminder_enable", true);

        paymentLinkRequest.put("callback_url", "http://localhost:5173/wallet?order_id="+orderId);
        paymentLinkRequest.put("callback_method", "get");
        return paymentLinkRequest;
    }

    @Override
    public PaymentResponse createStripePaymentLink(User user, Long amount, Long orderId) throws StripeException {
        Stripe.apiKey = stripSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:5455/wallet?order_id="+orderId)
                .setCancelUrl("http://loclhost:5455/payment/canel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amount*100)
                                .setProductData(SessionCreateParams
                                        .LineItem
                                        .PriceData
                                        .ProductData
                                        .builder()
                                        .setName("Top up wallet")
                                        .build()
                                ).build()
                        ).build()
                ).build();

        Session session = Session.create(params);

        System.out.println("session ____ " + session);

        PaymentResponse res = new PaymentResponse();
        res.setPaymentUrl(session.getUrl());
        return res;
    }
}
