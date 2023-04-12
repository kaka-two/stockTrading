package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.bars.SecBar;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SecBarMapper extends BaseMapper<SecBar> {
}
