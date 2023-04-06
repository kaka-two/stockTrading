package com.kakas.stockTrading.service;

import com.kakas.stockTrading.bean.SimpleMatchDetailBean;
import com.kakas.stockTrading.dbService.MatchDetailServiceImpl;
import com.kakas.stockTrading.dbService.OrderServiceImpl;
import com.kakas.stockTrading.pojo.MatchDetail;
import com.kakas.stockTrading.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HistoryService {
    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private MatchDetailServiceImpl matchDetailService;

    public List<Order> getHistoryOrders(Long userId, int maxCount) {
        return orderService.getOrders(userId, maxCount);
    }

    public Order getHistoryOrder(Long userId, Long orderId) {
        return orderService.getOrder(userId, orderId);
    }

    public List<SimpleMatchDetailBean> getHistoryMatchDetails(Long orderId) {
        List<MatchDetail> list = matchDetailService.getMatchDetails(orderId);
        return list.stream().map(e -> new SimpleMatchDetailBean(e.getPrice(), e.getQuantity(), e.getType()))
                .collect(Collectors.toList());
    }

}
