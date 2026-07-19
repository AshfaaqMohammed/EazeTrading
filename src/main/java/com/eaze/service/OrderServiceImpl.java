package com.eaze.service;

import com.eaze.domian.OrderStatus;
import com.eaze.domian.OrderType;
import com.eaze.model.Coin;
import com.eaze.model.Order;
import com.eaze.model.OrderItem;
import com.eaze.model.User;
import com.eaze.repository.OrderItemRepository;
import com.eaze.repository.OrderRepository;
import com.eaze.service.domain.OrderService;
import com.eaze.service.domain.WalletService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository, WalletService walletService, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.walletService = walletService;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        double price = orderItem.getCoin().getCurrentPrice() * orderItem.getQuantity();

        Order order = new Order();
        order.setUser(user);
        order.setOrderItem(orderItem);
        order.setOrderType(orderType);
        order.setPrice(BigDecimal.valueOf(price));
        order.setTimestamp(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING);

        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) throws Exception {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isPresent()) {
            return order.get();
        }
        throw new Exception("Order not found!!");
    }

    @Override
    public List<Order> getAllOrdersOfUser(Long userId, OrderType orderType, String assetSymbol) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Order processOrder(Coin coin, double quantity, OrderType orderType, User user) throws Exception {
        if (orderType.equals(OrderType.BUY)) {
            return buyAsset(coin, quantity, user);
        }else if (orderType.equals(OrderType.SELL)) {
            return sellAsset(coin, quantity, user);
        }
        throw new Exception("No such OrderType - "+orderType);
    }

    private OrderItem createOrderItem(Coin coin, double quantity, double buyPrice, double sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);

        return orderItemRepository.save(orderItem);
    }

    @Transactional
    protected Order buyAsset(Coin coin, double quantity, User user) throws Exception {
        if (quantity<=0) {
            throw new Exception("Quantity must not be 0 or <0");
        }
        double buyPrice = coin.getCurrentPrice();
        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, 0);
        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);

        walletService.payOrderPayment(order, user);
        order.setOrderStatus(OrderStatus.SUCCESS);

        Order savedOrder = orderRepository.save(order);

        // TODO: create asset
        return savedOrder;
    }

    @Transactional
    protected Order sellAsset(Coin coin, double quantity, User user) throws Exception {
        if (quantity <=0 ) {
            throw new Exception("Quantity must not be 0 or <0");
        }

        double sellPrice = coin.getCurrentPrice();
        OrderItem orderItem = createOrderItem(coin, quantity, 0, sellPrice);
        Order order = createOrder(user, orderItem, OrderType.SELL);
        orderItem.setOrder(order);

        walletService.payOrderPayment(order, user);
        order.setOrderStatus(OrderStatus.SUCCESS);

        Order savedOrder = orderRepository.save(order);

        //TODO create asset (5:53:00)
        return savedOrder;
    }
}
