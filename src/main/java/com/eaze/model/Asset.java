package com.eaze.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private BigDecimal quantity;
    private BigDecimal buyPrice;

    @ManyToOne
    private Coin coin;

    @ManyToOne
    private User user;
}
