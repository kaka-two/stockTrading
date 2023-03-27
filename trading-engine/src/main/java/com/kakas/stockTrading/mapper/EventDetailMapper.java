package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.pojo.EventDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EventDetailMapper extends BaseMapper<EventDetail> {

}

