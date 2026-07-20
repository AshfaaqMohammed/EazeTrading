package com.eaze.service.domain;

import com.eaze.model.Coin;
import com.eaze.model.User;
import com.eaze.model.WatchList;

public interface WatchListService {

    WatchList findUserWatchList(Long userId) throws Exception;

    WatchList createWatchList(User user);

    WatchList findById(Long id) throws Exception;

    Coin addItemToWatchList(Coin coin, User user) throws Exception;

}
