package com.eaze.model;

import com.eaze.domian.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class WalletTransaction {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    private LocalDateTime date;

    private String transferId;

    private String purpose;

    private BigDecimal amount;
}
