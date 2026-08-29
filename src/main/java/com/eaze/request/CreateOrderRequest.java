package com.eaze.request;

import com.eaze.domian.OrderType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private String coinId;
    private BigDecimal quantity;
    private OrderType orderType;
}
