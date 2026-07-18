package com.eaze.controller;

import com.eaze.model.Coin;
import com.eaze.service.domain.CoinService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/coins")
public class CoinController {
    private final CoinService coinService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @GetMapping
    ResponseEntity<List<Coin>> getCoinList(@RequestParam("page") int page) throws Exception  {
        List<Coin> coinList = coinService.getCoinList(page);
        return new ResponseEntity<>(coinList, HttpStatus.ACCEPTED);
    }

    @GetMapping("/{coinId}/chart")
    ResponseEntity<JsonNode> getMarketChart(@RequestParam("days")int days, @PathVariable("coinId") String coinId) throws Exception {
        String marketChart = coinService.getMarketChart(coinId, days);
        JsonNode jsonNode = objectMapper.readTree(marketChart);
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);
    }

    @GetMapping("/details/{coinId}")
    ResponseEntity<JsonNode> getCoinDetails(@PathVariable("coinId") String coinId) throws Exception {
        String coinDetails = coinService.getCoinDetails(coinId);
        JsonNode jsonNode = objectMapper.readTree(coinDetails);
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);
    }

    @GetMapping("/{coinId}")
    ResponseEntity<Coin> findById(@PathVariable("coinId") String coinId) throws Exception {
        Coin coin = coinService.findById(coinId);
        return new ResponseEntity<>(coin, HttpStatus.ACCEPTED);
    }

    @GetMapping("/search")
    ResponseEntity<JsonNode> searchCoin(@RequestParam("q") String keyWord) throws Exception {
        String coin = coinService.searchCoin(keyWord);
        JsonNode jsonNode = objectMapper.readTree(coin);
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);
    }

    @GetMapping("/top50")
    ResponseEntity<JsonNode> getTop50CoinsByMarketCapRank() throws Exception {
        String top50 = coinService.getTop50CoinsByMarketCapRank();
        JsonNode jsonNode = objectMapper.readTree(top50);
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);
    }

    @GetMapping("/trending")
    ResponseEntity<JsonNode> getTrendingCoins() throws Exception {
        String tradingCoins = coinService.getTrendingCoins();
        JsonNode jsonNode = objectMapper.readTree(tradingCoins);
        return new ResponseEntity<>(jsonNode, HttpStatus.ACCEPTED);
    }




}
