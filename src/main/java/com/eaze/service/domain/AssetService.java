package com.eaze.service.domain;

import com.eaze.model.Asset;
import com.eaze.model.Coin;
import com.eaze.model.User;

import java.util.List;

public interface AssetService {

    Asset createAsset(User user, Coin coin, double quantity);

    Asset getAssetById(Long assetId) throws Exception;

    Asset getAssetByUserIdAndId(Long userId, Long assetId);

    List<Asset> getUsersAssets(Long userId);

    Asset updateAsset(Long assetId, double quantity) throws Exception;

    Asset findAssetByUserIdAndCoinId(Long userid, String coinId);

    void deleteAsset(Long assetId);
}
