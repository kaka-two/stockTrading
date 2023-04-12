package com.kakas.stockTrading.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakas.stockTrading.bean.OrderBookBean;
import com.kakas.stockTrading.bean.OrderRequestBean;
import com.kakas.stockTrading.bean.SimpleMatchDetailBean;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.message.ApiMessage;
import com.kakas.stockTrading.message.event.OrderCancelEvent;
import com.kakas.stockTrading.message.event.OrderRequestEvent;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.redis.RedisKey;
import com.kakas.stockTrading.redis.RedisService;
import com.kakas.stockTrading.redis.RedisTopic;
import com.kakas.stockTrading.service.HistoryService;
import com.kakas.stockTrading.service.SendEventService;
import com.kakas.stockTrading.service.TradingEngineApiProxyService;
import com.kakas.stockTrading.util.IdUtil;
import com.kakas.stockTrading.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@Slf4j
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

    private Long asyncTimeout = Long.valueOf(500);

    Map<String, DeferredResult<ResponseEntity<String>>> asyncResultsMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.redisService.subscribe(RedisTopic.TRADING_RESULT.name(), this::onTradingResultMessage);
    }

    // 处理API的交易结果消息
    public void onTradingResultMessage(String message) {
        log.info("on subscribe message: {}", message);
        try {
            ApiMessage apiMessage = JsonUtil.readJson(message, ApiMessage.class);
            if (apiMessage.getRefId() == null) {
                return;
            }
            DeferredResult<ResponseEntity<String>> deferred = this.asyncResultsMap.get(apiMessage.getRefId());
            if (deferred == null) {
                return;
            }
            if (apiMessage.getError() != null) {
                String error = JsonUtil.writeJson(apiMessage.getError());
                ResponseEntity<String> resp = new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
                deferred.setResult(resp);
            } else {
                String result = JsonUtil.writeJson(apiMessage.getResult());
                ResponseEntity<String> resp = new ResponseEntity<>(result, HttpStatus.OK);
                deferred.setResult(resp);
            }
        } catch (Exception e) {
            log.error("Invalid ApiResultMessage: " + message);
            throw new RuntimeException(e);
        }

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

    @GetMapping(value = "/history/orders/{orderId}/matches", produces = "application/json")
    public List<SimpleMatchDetailBean> getOrderMatchDetails(@PathVariable("orderId") Long orderId) throws IOException {
        // 查找活动订单
        String openOrder = tradingEngineApiProxyService.get("/internal/" + UserContext.getRequiredUserId() + "/orders/" + orderId);
        // 从历史订单中查找
        if (openOrder.equals("null")) {
            Order order = historyService.getHistoryOrder(UserContext.getRequiredUserId(), orderId);
            // 如果历史订单中也没有，抛出异常
            if (order == null) {
                throw new RuntimeException("Order not found");
            }
        }
        return historyService.getHistoryMatchDetails(orderId);
    }

    @PostMapping(value = "/orders/{orderId}/cancel", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> cancelOrder(@PathVariable("orderId") Long orderId) throws IOException {
        Long userId = UserContext.getRequiredUserId();
        String openOrder = tradingEngineApiProxyService.get("/internal/" + userId + "/orders/" + orderId);
        if (openOrder.equals("null")) {
            throw new RuntimeException("Order not found");
        }
        // 创建取消订单的消息
        String refId = IdUtil.generateUniqueId();
        var event = new OrderCancelEvent();
        event.setUserId(userId);
        event.setRefId(refId);
        event.setRefOrderId(orderId);
        event.setCreatedAt(System.currentTimeMillis());
        // 创建DeferredResult
        ResponseEntity<String> timeout = new ResponseEntity<>("Operate time out" , HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(this.asyncTimeout, timeout);
        deferredResult.onTimeout(() -> {
            log.warn("Cancel order timeout, userId: {}, orderId : {}", userId, orderId);
            this.asyncResultsMap.remove(refId);
        });
        // 之后会根据refId从map中取出DeferredResult
        this.asyncResultsMap.put(refId, deferredResult);
        log.info("Cancel order message created, userId: {}, orderId : {}", userId, orderId);
        sendEventService.sendEvent(event);
        return deferredResult;
    }

    @PostMapping(value = "/orders", produces = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<String>> createOrder(@RequestBody OrderRequestBean orderRequest) throws IOException {
        orderRequest.validate();
        Long userId = UserContext.getRequiredUserId();
        // 创建取消订单的消息
        String refId = IdUtil.generateUniqueId();
        var event = new OrderRequestEvent();
        event.setRefId(refId);
        event.setCreatedAt(System.currentTimeMillis());
        event.setUserId(userId);
        event.setDirection(orderRequest.getDirection());
        event.setPrice(orderRequest.getPrice());
        event.setQuantity(orderRequest.getQuantity());
        // 创建DeferredResult
        ResponseEntity<String> timeout = new ResponseEntity<>("Operate time out" , HttpStatus.BAD_REQUEST);
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(this.asyncTimeout, timeout);
        deferredResult.onTimeout(() -> {
            log.warn("Create order timeout, userId: {}, refId : {}", userId, refId);
            this.asyncResultsMap.remove(refId);
        });
        // 之后会根据refId从map中取出DeferredResult
        this.asyncResultsMap.put(refId, deferredResult);
        log.info("Create order message created, userId: {}", userId);
        sendEventService.sendEvent(event);
        return deferredResult;
    }

}

