package com.eaze.service;

import com.eaze.domian.OrderType;
import com.eaze.model.*;
import com.eaze.repository.OrderRepository;
import com.eaze.service.domain.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int MAX_ATTEMPTS = 3;

    private final OrderRepository orderRepository;
    private final OrderTransactionalService orderTransactionalService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        return orderTransactionalService.createOrder(user, orderItem, orderType);
    }

    @Override
    public Order getOrderById(Long orderId) throws Exception {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isPresent()) {
            return order.get();
        }
        throw new Exception("order not found!!");
    }

    @Override
    public List<Order> getAllOrdersOfUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user) throws Exception {
        OptimisticLockingFailureException lastConflict = null;

        for (int attempt=1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return orderTransactionalService.processOrderOnce(coin, quantity, orderType, user);
            } catch (OptimisticLockingFailureException ex) {
                lastConflict = ex;
            }
        }
        throw new Exception("Could not complete the order after " + MAX_ATTEMPTS
                + " attempts due to concurrent modification. Please try again.", lastConflict);
    }
}
