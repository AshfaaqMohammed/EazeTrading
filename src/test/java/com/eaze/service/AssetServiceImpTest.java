package com.eaze.service;

import com.eaze.model.Asset;
import com.eaze.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceImpTest {

    @Mock private AssetRepository assetRepository;

    @InjectMocks private AssetServiceImp assetService;

    private Asset asset(Long id, BigDecimal qty, BigDecimal buyPrice) {
        Asset a = new Asset();
        a.setId(id);
        a.setQuantity(qty);
        a.setBuyPrice(buyPrice);
        return a;
    }

    @Test
    void updateAssetOnBuy_recomputesWeightedAverage() throws Exception {
        // hold 1 @ 100, buy 1 more @ 200 -> avg 150
        Asset existing = asset(5L, new BigDecimal("1"), new BigDecimal("100"));
        when(assetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Asset result = assetService.updateAssetOnBuy(5L, new BigDecimal("1"), new BigDecimal("200"));

        assertEquals(0, new BigDecimal("2").compareTo(result.getQuantity()));
        assertEquals(0, new BigDecimal("150").compareTo(result.getBuyPrice()),
                "weighted-average basis should be (1*100 + 1*200)/2 = 150");
    }

    @Test
    void updateAssetOnBuy_unequalQuantities_weightedCorrectly() throws Exception {
        // hold 2 @ 100, buy 3 @ 200 -> (2*100 + 3*200)/5 = 800/5 = 160
        Asset existing = asset(5L, new BigDecimal("2"), new BigDecimal("100"));
        when(assetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Asset result = assetService.updateAssetOnBuy(5L, new BigDecimal("3"), new BigDecimal("200"));

        assertEquals(0, new BigDecimal("5").compareTo(result.getQuantity()));
        assertEquals(0, new BigDecimal("160").compareTo(result.getBuyPrice()));
    }

    @Test
    void updateAssetOnBuy_nonTerminatingDivision_roundsWithoutError() throws Exception {
        // hold 1 @ 100, buy 2 @ 100 -> 300/3 = 100 exactly, but check a rounding case:
        // hold 1 @ 100, buy 1 @ 101 -> 201/2 = 100.5 (terminating) — use a 1/3 case:
        // hold 1 @ 10, buy 2 @ 10 with qty producing 1/3: total 30 / 3 = 10 (clean).
        // Force non-terminating: total 100 / 3 quantity.
        Asset existing = asset(5L, new BigDecimal("1"), new BigDecimal("10"));
        when(assetRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        // buy 2 @ 20 -> (1*10 + 2*20)/3 = 50/3 = 16.66666667 (scale 8, HALF_UP)
        Asset result = assetService.updateAssetOnBuy(5L, new BigDecimal("2"), new BigDecimal("20"));

        assertEquals(0, new BigDecimal("3").compareTo(result.getQuantity()));
        assertEquals(new BigDecimal("16.66666667"), result.getBuyPrice(),
                "should round to scale 8 HALF_UP without ArithmeticException");
    }

    @Test
    void updateAssetOnBuy_throws_whenAssetMissing() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class,
                () -> assetService.updateAssetOnBuy(99L, new BigDecimal("1"), new BigDecimal("100")));
    }
}
