package com.eaze.controller;

import com.eaze.model.Coin;
import com.eaze.service.domain.CoinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class HomeController {

    private final CoinService coinService;

    public HomeController(CoinService coinService) {
        this.coinService = coinService;
    }

    @GetMapping
    public String home() {
        return "welcome to home page";
    }

    @GetMapping("/coins-list")
    public ResponseEntity<List<Coin>> getCoinList(
            @RequestParam int page) throws Exception {
        return new ResponseEntity<>(coinService.getCoinList(page), HttpStatus.OK);
    }
}
