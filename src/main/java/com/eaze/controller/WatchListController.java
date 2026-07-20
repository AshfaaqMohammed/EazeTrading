package com.eaze.controller;

import com.eaze.model.Coin;
import com.eaze.model.User;
import com.eaze.model.WatchList;
import com.eaze.service.domain.CoinService;
import com.eaze.service.domain.UserService;
import com.eaze.service.domain.WatchListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchListController {

    private final WatchListService watchListService;
    private final UserService userService;
    private final CoinService coinService;

    @GetMapping("/user")
    public ResponseEntity<WatchList> getUserWatchList(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        WatchList watchList = watchListService.findUserWatchList(user.getId());

        return new ResponseEntity<>(watchList, HttpStatus.OK);
    }

    @GetMapping("/create")
    public ResponseEntity<WatchList> createWatchList(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        WatchList createWatchList = watchListService.createWatchList(user);

        return new ResponseEntity<>(createWatchList, HttpStatus.CREATED);
    }

    @GetMapping("/{watchListId}")
    public ResponseEntity<WatchList> getWatchListById(@PathVariable("watchListId") Long watchListId) throws Exception {
        WatchList watchList = watchListService.findById(watchListId);
        return new ResponseEntity<>(watchList, HttpStatus.OK);
    }

    @PatchMapping("/add/coin/{coinId}")
    public ResponseEntity<Coin> addItemToWatchList(@RequestHeader("Authorization") String jwt,
                                                   @PathVariable("coinId") String coinId) throws Exception {

        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(coinId);
        Coin addedCoin = watchListService.addItemToWatchList(coin, user);
        return new ResponseEntity<>(addedCoin, HttpStatus.OK);
    }

}
