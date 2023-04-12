package com.kakas.stockTrading.dbService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.TickMapper;
import com.kakas.stockTrading.pojo.Tick;
import org.springframework.stereotype.Component;

@Component
public class TickServiceImpl extends ServiceImpl<TickMapper, Tick> {
}
