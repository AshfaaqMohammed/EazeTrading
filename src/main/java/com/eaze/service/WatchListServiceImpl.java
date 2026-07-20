package com.eaze.service;

import com.eaze.model.Coin;
import com.eaze.model.User;
import com.eaze.model.WatchList;
import com.eaze.repository.WatchListRepository;
import com.eaze.service.domain.WatchListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchListServiceImpl implements WatchListService {

    private final WatchListRepository watchListRepository;

    @Override
    public WatchList findUserWatchList(Long userId) throws Exception {
        WatchList watchList = watchListRepository.findByUserId(userId);
        if (watchList == null) {
            throw new Exception("WatchList not found!!");
        }
        return watchList;
    }

    @Override
    public WatchList createWatchList(User user) {
        WatchList newWatchList = new WatchList();
        newWatchList.setUser(user);

        return watchListRepository.save(newWatchList);
    }

    @Override
    public WatchList findById(Long id) throws Exception {
        Optional<WatchList> watchList =  watchListRepository.findById(id);
        if (watchList.isEmpty()) {
            throw new Exception("WatchList not found!!!");
        }
        return watchList.get();
    }

    @Override
    public Coin addItemToWatchList(Coin coin, User user) throws Exception {
        WatchList watchList = findUserWatchList(user.getId());

        if (watchList.getCoins().contains(coin)) {
            watchList.getCoins().remove(coin);
        }else {
            watchList.getCoins().add(coin);
        }
        watchListRepository.save(watchList);
        return coin;
    }
}
