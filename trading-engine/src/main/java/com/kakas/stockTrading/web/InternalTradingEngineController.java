package com.kakas.stockTrading.web;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.pojo.Assert;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.service.AssertService;
import com.kakas.stockTrading.service.OrderOpeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal")
public class InternalTradingEngineController {
    @Autowired
    private OrderOpeService orderOpeService;

    @Autowired
    private AssertService assertService;

    @RequestMapping("/{userId}/asserts")
    public Map<AssertType, Assert> getAssert(@PathVariable("userId") Long userId) {
        Map<AssertType, Assert> map =  assertService.getAsserts(userId);
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return map;
    }

    @RequestMapping("/{userId}/orders")
    public List<Order> getOrders(@PathVariable("userId") Long userId) {
        Map<Long, Order> map =  orderOpeService.getUserOrders(userId);
        if (map == null || map.isEmpty()) {
            return List.of();
        }
        return map.values().stream().map(Order::copyOrder).collect(Collectors.toList());
    }

    @RequestMapping("/{userId}/orders/{orderId}")
    public Order getOrder(@PathVariable("userId") Long userId, @PathVariable("orderId") Long orderId) {
        Order order = orderOpeService.getOrder(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return order.copyOrder();
    }
}
