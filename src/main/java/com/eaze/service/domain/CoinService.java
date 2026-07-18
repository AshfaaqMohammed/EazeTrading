package com.eaze.service.domain;

import com.eaze.model.Coin;

import java.util.List;

public interface CoinService {

    List<Coin> getCoinList(int page) throws Exception;

    String getMarketChart(String coinId, int days) throws Exception;

    String getCoinDetails(String coinId) throws Exception;

    Coin findById(String coinId) throws Exception;

    String searchCoin(String keyWord) throws Exception;

    String getTop50CoinsByMarketCapRank() throws Exception;

    String getTrendingCoins() throws Exception;
}
