package com.eaze.service.domain;

import com.eaze.domian.OrderType;
import com.eaze.model.Coin;
import com.eaze.model.Order;
import com.eaze.model.OrderItem;
import com.eaze.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    Order createOrder(User user, OrderItem orderItem, OrderType orderType);
    Order getOrderById(Long orderId) throws Exception;
    List<Order> getAllOrdersOfUser(Long userId);
    Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user) throws Exception;
}
