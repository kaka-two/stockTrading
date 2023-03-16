package com.kakas.stockTrading.service;

import org.springframework.beans.factory.annotation.Autowired;

public class TradingEngineService {
    @Autowired
    AssertService assertService;

    @Autowired
    OrderService orderService;

    @Autowired
    MatchService matchService;

    @Autowired
    ClearService clearService;


}
