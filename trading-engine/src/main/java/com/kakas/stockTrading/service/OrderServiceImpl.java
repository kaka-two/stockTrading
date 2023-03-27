package com.kakas.stockTrading.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.OrderMapper;
import com.kakas.stockTrading.pojo.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> {
}
