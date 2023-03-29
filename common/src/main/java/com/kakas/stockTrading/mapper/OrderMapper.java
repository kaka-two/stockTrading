package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

}