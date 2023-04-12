package com.kakas.stockTrading.service;

import com.kakas.stockTrading.dbService.TickService;
import com.kakas.stockTrading.dbService.TickServiceImpl;
import com.kakas.stockTrading.mapper.*;
import com.kakas.stockTrading.pojo.Tick;
import com.kakas.stockTrading.pojo.bars.DarBar;
import com.kakas.stockTrading.pojo.bars.HourBar;
import com.kakas.stockTrading.pojo.bars.MinBar;
import com.kakas.stockTrading.pojo.bars.SecBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class QuotationDbService {
    @Autowired
    DarBarMapper darBarMapper;

    @Autowired
    HourBarMapper hourBarMapper;

    @Autowired
    MinBarMapper minBarMapper;

    @Autowired
    SecBarMapper secBarMapper;

    @Autowired
    TickServiceImpl tickService;

    public void saveBars(DarBar darBar, HourBar hourBar, MinBar minBar, SecBar secBar) {
        darBarMapper.insert(darBar);
        hourBarMapper.insert(hourBar);
        minBarMapper.insert(minBar);
        secBarMapper.insert(secBar);
    }

    public void saveTicks(List<Tick> ticks) {
        tickService.saveBatch(ticks);
    }
}
