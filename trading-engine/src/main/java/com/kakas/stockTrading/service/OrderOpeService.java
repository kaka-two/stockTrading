package com.kakas.stockTrading.service;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderOpeService {
    private AssertService assertService;

    public OrderOpeService(@Autowired AssertService assertService) {
        this.assertService = assertService;
    }
    // 活跃订单
    ConcurrentMap<Long, Order> activeOrder = new ConcurrentHashMap<>();
    // 用户的所有活跃订单
    ConcurrentMap<Long, ConcurrentMap<Long, Order>> userOrders = new ConcurrentHashMap<>();

    // 创建订单
    public Order createOrder(Long userId, long sequenceId, Direction direction, BigDecimal price, BigDecimal quantity, long createAt) {
        // 冻结资产
        switch (direction) {
            case SELL -> {
                if (!assertService.freeze(userId, AssertType.StockA, quantity)) {
                    return null;
                }
            }
            case BUY -> {
                if (!assertService.freeze(userId, AssertType.Money, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        }
        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setSequenceId(sequenceId);
        order.setDirection(direction);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setCreateAt(createAt);
        order.setUpdateAt(createAt);
        // 添加到活跃订单和用户活跃订单
        activeOrder.put(order.getOrderId(), order);
        ConcurrentMap<Long, Order> map = userOrders.getOrDefault(userId, new ConcurrentHashMap<>());
        map.put(order.getOrderId(), order);
        userOrders.put(userId, map);
        return order;
    }


    // 删除订单
    public void removeOrder(Long orderId) {
        // 从活跃订单中删除
        if (!activeOrder.containsKey(orderId)) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        Order order = activeOrder.remove(orderId);
        // 从用户活跃订单中删除
        ConcurrentMap<Long, Order> map = userOrders.getOrDefault(order.getUserId(), new ConcurrentHashMap<>());
        if (!map.containsKey(orderId)) {
            throw new IllegalArgumentException("Order not found by orderId in user active orders: " + orderId);
        }
        map.remove(orderId);
    }

    // 查询活跃订单
    public Order getOrder(Long orderId) {
        if (!activeOrder.containsKey(orderId)) {
            return null;
        }
        return activeOrder.get(orderId);
    }

    // 查询用户所有活跃订单
    public ConcurrentMap<Long, Order> getUserOrders(Long userId) {
        return userOrders.getOrDefault(userId, new ConcurrentHashMap<>());
    }
}
