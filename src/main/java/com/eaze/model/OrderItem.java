package com.eaze.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class OrderItem {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private BigDecimal quantity;

    @ManyToOne
    private Coin coin;

    private BigDecimal buyPrice;

    private BigDecimal sellPrice;

    @JsonIgnore
    @OneToOne
    private Order order;
}
