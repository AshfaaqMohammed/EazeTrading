package com.eaze.model;

import com.eaze.domian.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private WithdrawalStatus status;
    private BigDecimal amount;

    @ManyToOne
    private User user;

    private LocalDateTime date = LocalDateTime.now();
}
