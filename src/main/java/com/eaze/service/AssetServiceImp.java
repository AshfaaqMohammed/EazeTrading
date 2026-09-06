package com.eaze.service;

import com.eaze.model.Asset;
import com.eaze.model.Coin;
import com.eaze.model.User;
import com.eaze.repository.AssetRepository;
import com.eaze.service.domain.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetServiceImp implements AssetService {

    private static final int COST_BASIS_SCALE = 8;

    private final AssetRepository assetRepository;

    @Override
    public Asset createAsset(User user, Coin coin, BigDecimal quantity) {
        Asset asset = new Asset();
        asset.setUser(user);
        asset.setCoin(coin);
        asset.setQuantity(quantity);
        asset.setBuyPrice(BigDecimal.valueOf(coin.getCurrentPrice()));
        return assetRepository.save(asset);
    }

    @Override
    public Asset getAssetById(Long assetId) throws Exception {
        Optional<Asset> asset = assetRepository.findById(assetId);
        if (asset.isPresent()) {
            return asset.get();
        }
        throw new Exception("No asset found!!");
    }

    @Override
    public Asset getAssetByUserIdAndId(Long userId, Long assetId) {
        return null;
    }

    @Override
    public List<Asset> getUsersAssets(Long userId) {
        return assetRepository.findByUserId(userId);
    }

    @Override
    public Asset updateAsset(Long assetId, BigDecimal quantity) throws Exception {
        Asset oldAsset = getAssetById(assetId);

        oldAsset.setQuantity(oldAsset.getQuantity().add(quantity));

        return assetRepository.save(oldAsset);
    }

    @Override
    public Asset updateAssetOnBuy(Long assetId, BigDecimal addedQuantity, BigDecimal buyPrice) throws Exception {
        Asset oldAsset = getAssetById(assetId);

        BigDecimal oldQuantity = oldAsset.getQuantity();
        BigDecimal oldBuyPrice = oldAsset.getBuyPrice();
        BigDecimal newQuantity = oldQuantity.add(addedQuantity);

        // Weighted-average cost basis:
        // newBuyPrice = (oldQty * oldBuyPrice + addedQty * buyPrice) / (oldQty + addedQty)
        if (newQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalCost = oldQuantity.multiply(oldBuyPrice)
                    .add(addedQuantity.multiply(buyPrice));
            BigDecimal newBuyPrice = totalCost.divide(newQuantity, COST_BASIS_SCALE, RoundingMode.HALF_UP);
            oldAsset.setBuyPrice(newBuyPrice);
        }

        oldAsset.setQuantity(newQuantity);

        return assetRepository.save(oldAsset);
    }

    @Override
    public Asset findAssetByUserIdAndCoinId(Long userid, String coinId) {
        return assetRepository.findByUserIdAndCoinId(userid, coinId);
    }

    @Override
    public void deleteAsset(Long assetId) {
        assetRepository.deleteById(assetId);
    }
}
