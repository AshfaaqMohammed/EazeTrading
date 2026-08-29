package com.eaze.model;

import com.eaze.domian.PaymentMethod;
import com.eaze.domian.PaymentOrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private BigDecimal amount;
    private PaymentOrderStatus status;
    private PaymentMethod paymentMethod;

    @ManyToOne
    private User user;
}
