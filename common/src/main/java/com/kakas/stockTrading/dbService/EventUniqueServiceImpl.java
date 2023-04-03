package com.kakas.stockTrading.dbService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.EventUniqueMapper;
import com.kakas.stockTrading.pojo.EventUnique;
import org.springframework.stereotype.Component;

@Component
public class EventUniqueServiceImpl extends ServiceImpl<EventUniqueMapper, EventUnique> {
    public EventUnique getEventUnique(String uniqueId) {
        QueryWrapper qw = new QueryWrapper();
        qw.eq("uniqueId", uniqueId);
        return this.getOne(qw);
    }
}
