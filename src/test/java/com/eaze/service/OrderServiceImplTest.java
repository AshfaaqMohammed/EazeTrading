package com.eaze.service;

import com.eaze.domian.OrderType;
import com.eaze.model.Asset;
import com.eaze.model.Coin;
import com.eaze.model.Order;
import com.eaze.model.OrderItem;
import com.eaze.model.User;
import com.eaze.model.Wallet;
import com.eaze.repository.OrderItemRepository;
import com.eaze.repository.OrderRepository;
import com.eaze.service.domain.AssetService;
import com.eaze.service.domain.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private WalletService walletService;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AssetService assetService;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Coin coin;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(1L);

        coin = new Coin();
        coin.setId("bitcoin");
        coin.setCurrentPrice(100.0);

        lenient().when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(walletService.payOrderPayment(any(Order.class), any(User.class)))
                .thenReturn(new Wallet());
    }

    // ---------- BUY ----------

    @Test
    void buy_firstTime_createsAsset() throws Exception {
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(null);

        orderService.processOrder(coin, new BigDecimal("2"), OrderType.BUY, user);

        // first buy -> createAsset, never weighted-average update
        verify(assetService).createAsset(eq(user), eq(coin), eq(new BigDecimal("2")));
        verify(assetService, never()).updateAssetOnBuy(anyLong(), any(), any());
    }

    @Test
    void buy_repeat_usesWeightedAverageUpdate_notPlainUpdate() throws Exception {
        Asset existing = new Asset();
        existing.setId(5L);
        existing.setQuantity(new BigDecimal("1"));
        existing.setBuyPrice(new BigDecimal("100"));
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(existing);

        // buy 1 more at current price 100
        orderService.processOrder(coin, new BigDecimal("1"), OrderType.BUY, user);

        // must recompute cost basis via updateAssetOnBuy(assetId, addedQty, buyPrice=currentPrice)
        verify(assetService).updateAssetOnBuy(eq(5L), eq(new BigDecimal("1")), eq(BigDecimal.valueOf(100.0)));
        verify(assetService, never()).updateAsset(anyLong(), any());
    }

    @Test
    void buy_rejects_zeroOrNegativeQuantity() {
        assertThrows(Exception.class,
                () -> orderService.processOrder(coin, BigDecimal.ZERO, OrderType.BUY, user));
        assertThrows(Exception.class,
                () -> orderService.processOrder(coin, new BigDecimal("-1"), OrderType.BUY, user));
    }

    // ---------- SELL ----------

    @Test
    void sell_rejects_whenHoldingLessThanRequested() throws Exception {
        Asset held = new Asset();
        held.setId(5L);
        held.setQuantity(new BigDecimal("1"));
        held.setBuyPrice(new BigDecimal("100"));
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(held);

        Exception ex = assertThrows(Exception.class,
                () -> orderService.processOrder(coin, new BigDecimal("5"), OrderType.SELL, user));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    void sell_rejects_whenNoAsset() {
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(null);
        assertThrows(Exception.class,
                () -> orderService.processOrder(coin, new BigDecimal("1"), OrderType.SELL, user));
    }

    @Test
    void sell_partial_reducesQuantity_andDoesNotDeleteWhenQuantityRemains() throws Exception {
        Asset held = new Asset();
        held.setId(5L);
        held.setQuantity(new BigDecimal("10"));
        held.setBuyPrice(new BigDecimal("100"));
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(held);

        Asset afterUpdate = new Asset();
        afterUpdate.setId(5L);
        afterUpdate.setQuantity(new BigDecimal("8")); // still holds 8
        when(assetService.updateAsset(eq(5L), eq(new BigDecimal("2").negate()))).thenReturn(afterUpdate);

        orderService.processOrder(coin, new BigDecimal("2"), OrderType.SELL, user);

        verify(assetService).updateAsset(eq(5L), eq(new BigDecimal("2").negate()));
        // remaining quantity > 0 -> must NOT delete (no dollar-value dust deletion)
        verify(assetService, never()).deleteAsset(anyLong());
    }

    @Test
    void sell_all_deletesAsset_onlyWhenQuantityZero() throws Exception {
        Asset held = new Asset();
        held.setId(5L);
        held.setQuantity(new BigDecimal("2"));
        held.setBuyPrice(new BigDecimal("100"));
        when(assetService.findAssetByUserIdAndCoinId(1L, "bitcoin")).thenReturn(held);

        Asset afterUpdate = new Asset();
        afterUpdate.setId(5L);
        afterUpdate.setQuantity(BigDecimal.ZERO); // fully sold
        when(assetService.updateAsset(eq(5L), eq(new BigDecimal("2").negate()))).thenReturn(afterUpdate);

        orderService.processOrder(coin, new BigDecimal("2"), OrderType.SELL, user);

        verify(assetService).deleteAsset(5L);
    }

    @Test
    void sell_rejects_zeroOrNegativeQuantity() {
        assertThrows(Exception.class,
                () -> orderService.processOrder(coin, BigDecimal.ZERO, OrderType.SELL, user));
    }
}
