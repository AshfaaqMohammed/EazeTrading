package com.eaze.model;

import com.eaze.domian.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Withdrawal {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;
    private BigDecimal amount;

    @ManyToOne
    private User user;

    private LocalDateTime date = LocalDateTime.now();
}
