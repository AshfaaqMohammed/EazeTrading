package com.eaze.service;

import com.eaze.domian.OrderStatus;
import com.eaze.domian.OrderType;
import com.eaze.model.*;
import com.eaze.repository.OrderItemRepository;
import com.eaze.repository.OrderRepository;
import com.eaze.service.domain.AssetService;
import com.eaze.service.domain.OrderService;
import com.eaze.service.domain.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final OrderItemRepository orderItemRepository;
    private final AssetService assetService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        BigDecimal price = BigDecimal.valueOf(orderItem.getCoin().getCurrentPrice())
                .multiply(orderItem.getQuantity());

        Order order = new Order();
        order.setUser(user);
        order.setOrderItem(orderItem);
        order.setOrderType(orderType);
        order.setPrice(price);
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
    public List<Order> getAllOrdersOfUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Order processOrder(Coin coin, BigDecimal quantity, OrderType orderType, User user) throws Exception {
        if (orderType.equals(OrderType.BUY)) {
            return buyAsset(coin, quantity, user);
        }else if (orderType.equals(OrderType.SELL)) {
            return sellAsset(coin, quantity, user);
        }
        throw new Exception("No such OrderType - "+orderType);
    }

    private OrderItem createOrderItem(Coin coin, BigDecimal quantity, BigDecimal buyPrice, BigDecimal sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);

        return orderItemRepository.save(orderItem);
    }

    @Transactional
    protected Order buyAsset(Coin coin, BigDecimal quantity, User user) throws Exception {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Quantity must not be 0 or <0");
        }
        BigDecimal buyPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, BigDecimal.ZERO);
        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);

        walletService.payOrderPayment(order, user);
        order.setOrderStatus(OrderStatus.SUCCESS);

        Order savedOrder = orderRepository.save(order);

        Asset oldAsset = assetService.findAssetByUserIdAndCoinId(order.getUser().getId(), order.getOrderItem().getCoin().getId());

        if (oldAsset == null) {
            assetService.createAsset(user,orderItem.getCoin(),orderItem.getQuantity());
        }else {
            assetService.updateAsset(oldAsset.getId(), quantity);
        }

        return savedOrder;
    }

    @Transactional
    protected Order sellAsset(Coin coin, BigDecimal quantity, User user) throws Exception {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Quantity must not be 0 or <0");
        }

        BigDecimal sellPrice = BigDecimal.valueOf(coin.getCurrentPrice());

        Asset assetToSell = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());

        if (assetToSell!=null) {
            BigDecimal buyPrice = assetToSell.getBuyPrice();
            OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
            Order order = createOrder(user, orderItem, OrderType.SELL);
            orderItem.setOrder(order);

            if (assetToSell.getQuantity().compareTo(quantity) >= 0) {
                order.setOrderStatus(OrderStatus.SUCCESS);
                Order savedOrder = orderRepository.save(order);
                walletService.payOrderPayment(order, user);

                Asset updatedAsset = assetService.updateAsset(assetToSell.getId(), quantity.negate());
                if (updatedAsset.getQuantity().multiply(BigDecimal.valueOf(coin.getCurrentPrice())).compareTo(BigDecimal.ONE) <= 0) {
                    assetService.deleteAsset(updatedAsset.getId());
                }
                return savedOrder;
            }
            throw new Exception("Insufficient quantity to sell");
        }
        throw new Exception("Asset not found");

    }
}
