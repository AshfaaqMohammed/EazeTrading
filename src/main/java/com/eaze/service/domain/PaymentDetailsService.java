package com.eaze.service.domain;

import com.eaze.model.PaymentDetails;
import com.eaze.model.User;

public interface PaymentDetailsService {

    PaymentDetails addPaymentDetails(String accountNumber,
                                    String accountHolderName,
                                    String ifsc,
                                    String bankName,
                                    User user);

    public PaymentDetails getUserPaymentDetails(User user);

}
