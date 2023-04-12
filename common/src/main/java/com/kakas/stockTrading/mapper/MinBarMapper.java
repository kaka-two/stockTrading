package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.bars.MinBar;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MinBarMapper extends BaseMapper<MinBar> {
}
