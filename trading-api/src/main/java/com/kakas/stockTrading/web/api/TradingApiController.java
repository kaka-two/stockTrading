package com.kakas.stockTrading.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakas.stockTrading.bean.OrderBookBean;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.redis.RedisKey;
import com.kakas.stockTrading.redis.RedisService;
import com.kakas.stockTrading.redis.RedisTopic;
import com.kakas.stockTrading.service.HistoryService;
import com.kakas.stockTrading.service.SendEventService;
import com.kakas.stockTrading.service.TradingEngineApiProxyService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@RestController
@RequestMapping("/api")
public class TradingApiController {
    @Autowired
    HistoryService historyService;

    @Autowired
    SendEventService sendEventService;

    @Autowired
    TradingEngineApiProxyService tradingEngineApiProxyService;

    @Autowired
    RedisService redisService;

    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.redisService.subscribe(RedisTopic.TRADING_RESULT.name(), this::onTradingResultMessage);
    }

    // 处理API的交易结果消息
    public void onTradingResultMessage(String message) {

    }

    @GetMapping("/timestamp")
    public Map<String, Long> getTimestamp() {
        return Map.of("timestamp", Long.valueOf(System.currentTimeMillis()));
    }

    @ResponseBody
    @GetMapping(value = "/asserts", produces = "application/json")
    public String getAsserts() throws IOException {
        return tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/asserts");
    }

    @ResponseBody
    @GetMapping(value = "/orders", produces = "application/json")
    public String getOpenOrders() throws IOException {
        return tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/orders");
    }

    @ResponseBody
    @GetMapping(value = "/orders/{orderId}",  produces = "application/json")
    public String getOpenOrder(@PathVariable("orderId") Long orderId) throws IOException {
        return tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/orders/" + orderId);
    }

    @ResponseBody
    @GetMapping(value = "/orderbook", produces = "application/json")
    public String getOrderBook() {
        String orderBook = redisService.get(RedisKey.ORDER_BOOK.name());
        return orderBook == null ? OrderBookBean.Empty : orderBook;
    }

    @ResponseBody
    @GetMapping(value = "/ticks", produces = "application/json")
    public String getTicks() {
        List<String> ticks = redisService.lrange(RedisKey.RECENT_TICKS.name(), 0, -1);
        if (ticks == null || ticks.isEmpty()) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String tick : ticks) {
            sj.add(tick);
        }
        return sj.toString();
    }

    // 一年
    @ResponseBody
    @GetMapping(value = "/bars/day", produces = "application/json")
    public String getDayBars() {
        long end = System.currentTimeMillis();
        long start = end - 366 * 24 * 3600_000;
        return getBars(RedisKey.HOUR_BARS.name(), start, end);
    }

    // 一个月
    @ResponseBody
    @GetMapping(value = "/bars/hour", produces = "application/json")
    public String getHourBars() {
        long end = System.currentTimeMillis();
        long start = end - 30 * 24 * 3600_000;
        return getBars(RedisKey.HOUR_BARS.name(), start, end);
    }

    // 一天
    @ResponseBody
    @GetMapping(value = "/bars/min", produces = "application/json")
    public String getMinBars() {
        long end = System.currentTimeMillis();
        long start = end - 24 * 60 * 60_000;
        return getBars(RedisKey.MIN_BARS.name(), start, end);
    }

    // 一小时
    @ResponseBody
    @GetMapping(value = "/bars/sec", produces = "application/json")
    public String getSecBars() {
        long end = System.currentTimeMillis();
        long start = end - 60 * 60 * 1_000;
        return getBars(RedisKey.SEC_BARS.name(), start, end);
    }

    public String getBars(String key, long start, long end) {
        List<String> bars = redisService.zrangeByScore(key, start, end);
        if (bars == null || bars.isEmpty()) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (String bar : bars) {
            sj.add(bar);
        }
        return sj.toString();
    }

    @GetMapping(value = "/history/orders", produces = "application/json")
    public List<Order> getHistoryOrders(@RequestParam(value = "maxResults", defaultValue = "100") int maxResults) {
        if (maxResults < 1 || maxResults > 1000) {
            throw new RuntimeException("Invalid parameter : maxResults");
        }
        return historyService.getHistoryOrders(UserContext.getRequiredUserId(), maxResults);
    }





}

