package com.eaze.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Asset {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private BigDecimal quantity;
    private BigDecimal buyPrice;

    @ManyToOne
    private Coin coin;

    @ManyToOne
    private User user;
}
