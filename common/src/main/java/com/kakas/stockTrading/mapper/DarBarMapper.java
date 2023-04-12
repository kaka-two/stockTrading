package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.bars.DarBar;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DarBarMapper extends BaseMapper<DarBar> {
}
