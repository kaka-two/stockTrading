package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.Tick;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TickMapper extends BaseMapper<Tick> {
}
