package com.eaze.service;

import com.eaze.domian.OrderStatus;
import com.eaze.domian.OrderType;
import com.eaze.model.*;
import com.eaze.repository.OrderItemRepository;
import com.eaze.repository.OrderRepository;
import com.eaze.service.domain.AssetService;
import com.eaze.service.domain.WalletService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Holds the transactional "single attempt" of placing an order.
 *
 * Separated from OrderServiceImpl ON PURPOSE: the optimistic-lock RETRY LOOP lives
 * in OrderServiceImpl and calls processOrderOnce(...) THROUGH the Spring proxy, so
 * every retry starts a BRAND-NEW transaction. If the loop and this method were in
 * the same bean, the self-invocation would bypass the proxy and every "retry" would
 * reuse the same rollback-only transaction and fail forever.
 *
 * One call to processOrderOnce = one transaction = one attempt.
 */
@Service
@AllArgsConstructor
public class OrderTransactionalService {
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final OrderItemRepository orderItemRepository;
    private final AssetService assetService;

    @Transactional(rollbackFor = Exception.class)
    public Order processOrderOnce(Coin coin, BigDecimal quantity, OrderType orderType, User user) throws Exception {
        if (orderType.equals(OrderType.BUY)) {
            return buyAsset(coin, quantity, user);
        } else if (orderType.equals(OrderType.SELL)) {
            return sellAsset(coin, quantity, user);
        }
        throw new Exception("No such OrderType - " + orderType);
    }

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

    private OrderItem createOrderItem(Coin coin, BigDecimal quantity, BigDecimal buyPrice, BigDecimal sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);

        return orderItemRepository.save(orderItem);
    }

    private Order buyAsset(Coin coin, BigDecimal quantity, User user) throws Exception {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Quantity must not be 0 or <0");
        }
        BigDecimal buyPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, BigDecimal.ZERO);
        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);

        walletService.payOrderPayment(order, user);
        Asset oldAsset = assetService.findAssetByUserIdAndCoinId(order.getUser().getId(), order.getOrderItem().getCoin().getId());

        if (oldAsset == null) {
            assetService.createAsset(user, orderItem.getCoin(), orderItem.getQuantity());
        } else {
            // Recompute weighted-ated buys.
            assetService.updateAssetOnBuy(oldAsset.getId(), quantity, buyPrice);
        }
        return order;
    }

    private Order sellAsset(Coin coin, BigDecimal quantity, User user) throws Exception {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Quantity must not be 0 or <0");
        }

        BigDecimal sellPrice = BigDecimal.valueOf(coin.getCurrentPrice());

        Asset assetToSell = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());

        if (assetToSell != null) {
            BigDecimal buyPrice = assetToSell.getBuyPrice();
            OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
            Order order = createOrder(user, orderItem, OrderType.SELL);
            orderItem.setOrder(order);

            if (assetToSell.getQuantity().compareTo(quantity) >= 0) {
                walletService.payOrderPayment(order, user);
                Asset updateAsset = assetService.updateAsset(assetToSell.getId(), quantity.negate());
                if (updateAsset.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    assetService.deleteAsset(updateAsset.getId());
                }
                return order;
            }
            throw new Exception("Insufficient quantity to sell");
        }
        throw new Exception("Asset not found");
    }
}
