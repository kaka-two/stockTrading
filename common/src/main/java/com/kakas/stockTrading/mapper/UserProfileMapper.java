package com.kakas.stockTrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakas.stockTrading.pojo.Tick;
import com.kakas.stockTrading.pojo.UserProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
