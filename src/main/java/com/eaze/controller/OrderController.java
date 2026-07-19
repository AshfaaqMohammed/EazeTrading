package com.eaze.controller;

import com.eaze.domian.OrderType;
import com.eaze.model.Coin;
import com.eaze.model.Order;
import com.eaze.model.User;
import com.eaze.request.CreateOrderRequest;
import com.eaze.service.domain.CoinService;
import com.eaze.service.domain.OrderService;
import com.eaze.service.domain.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final CoinService coinService;

    public OrderController(OrderService orderService, UserService userService, CoinService coinService) {
        this.orderService = orderService;
        this.userService = userService;
        this.coinService = coinService;
    }

    @PostMapping("/pay")
    public ResponseEntity<Order> payOrderPayment (@RequestHeader("Authorization") String jwt,
                                                  @RequestBody CreateOrderRequest req) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Coin coin = coinService.findById(req.getCoinId());

        Order order = orderService.processOrder(coin, req.getQuantity(), req.getOrderType(), user);
        return new ResponseEntity<>(order,HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@RequestHeader("Authorization") String jwt,
                                              @PathVariable("orderId") Long orderId) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Order order = orderService.getOrderById(orderId);
        if (order.getUser().getId().equals(user.getId())) {
            return new ResponseEntity<>(order, HttpStatus.OK);
        }else {
            throw new Exception("You don't have access");
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrdersForUser(@RequestHeader("Authorization") String jwt,
                                                           @RequestParam(required = false) String orderType,
                                                           @RequestParam(required = false) String assetSymbol) throws Exception {

        Long userId = userService.findUserProfileByJwt(jwt).getId();
        List<Order> userOrder = orderService.getAllOrdersOfUser(userId, OrderType.valueOf(orderType), assetSymbol);

        return new ResponseEntity<>(userOrder,HttpStatus.OK);
    }





}
