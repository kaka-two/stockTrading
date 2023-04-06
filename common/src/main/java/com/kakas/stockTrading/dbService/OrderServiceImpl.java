package com.kakas.stockTrading.dbService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.OrderMapper;
import com.kakas.stockTrading.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> {
    @Autowired
    OrderMapper orderMapper;
    public List<Order> getOrders(Long userId, int maxCount) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).orderByDesc("order_id").last("limit " + maxCount);
        return orderMapper.selectList(queryWrapper);
    }

    public Order getOrder(Long userId, Long orderId) {
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("order_id", orderId).last("limit 1") ;
        return orderMapper.selectOne(queryWrapper);
    }


}
