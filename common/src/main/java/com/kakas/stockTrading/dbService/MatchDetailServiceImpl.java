package com.kakas.stockTrading.dbService;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.MatchDetailMapper;
import com.kakas.stockTrading.pojo.MatchDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatchDetailServiceImpl extends ServiceImpl<MatchDetailMapper, MatchDetail> {
    @Autowired
    MatchDetailMapper matchDetailMapper;

    public List<MatchDetail> getMatchDetails(Long orderId) {
        QueryWrapper<MatchDetail> qw = new QueryWrapper<>();
        qw.eq("order_id", orderId);
        return matchDetailMapper.selectList(qw);
    }
}
