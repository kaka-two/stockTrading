package com.kakas.stockTrading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kakas.stockTrading.enums.BarType;
import com.kakas.stockTrading.message.Message;
import com.kakas.stockTrading.message.TickMessage;
import com.kakas.stockTrading.messaging.KafkaTopic;
import com.kakas.stockTrading.messaging.MessageConsumer;
import com.kakas.stockTrading.messaging.MessagingFactory;
import com.kakas.stockTrading.pojo.Tick;
import com.kakas.stockTrading.pojo.bars.DarBar;
import com.kakas.stockTrading.pojo.bars.HourBar;
import com.kakas.stockTrading.pojo.bars.MinBar;
import com.kakas.stockTrading.pojo.bars.SecBar;
import com.kakas.stockTrading.redis.RedisKey;
import com.kakas.stockTrading.redis.RedisService;
import com.kakas.stockTrading.util.IpUtil;
import com.kakas.stockTrading.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


@Component
@Slf4j
public class QuotationService {
    @Autowired
    private ZoneId zoneId;

    @Autowired
    private RedisService redisService;

    @Autowired
    QuotationDbService quotationDbService;

    @Autowired
    private MessagingFactory messagingFactory;

    private MessageConsumer tickConsumer;

    private String shaUpdateRecentTicksLua = null;

    private String shaUpdateBarLua = null;

    // track last processed sequence id:
    private long sequenceId;

    @PostConstruct
    public void init() {
        // init redis
        this.shaUpdateRecentTicksLua = redisService.loadScriptFromClassPath("/redis/update-recent-ticks.lua");
        this.shaUpdateBarLua = redisService.loadScriptFromClassPath("/redis/update-bar.lua");
        // init kafka
        String groupId = KafkaTopic.TICK.name() + "-" + IpUtil.getHostId();
        this.tickConsumer = messagingFactory.createBatchMessageConsumer(KafkaTopic.TICK, groupId, this::processMessages);
    }

    @PreDestroy
    public void shutdown() {
        this.tickConsumer.stop();
    }

    public void processMessages(List<Message> messages) {
        for (Message message : messages) {
            this.processMessage((TickMessage) message);
        }
    }

    public void processMessage(TickMessage message) {
        // 忽略重复消息
        if (message.getSequenceId() < this.sequenceId) {
            return;
        }
        // 收集Tick，并合并成一个Bar [Tick, Tick,...]
        this.sequenceId = message.getSequenceId();
        final long createdAt = message.getCreatedAt();
        StringJoiner ticksStrJoiner = new StringJoiner(",", "[", "]");
        StringJoiner ticksJoiner = new StringJoiner(",", "[", "]");
        BigDecimal openPrice = BigDecimal.ZERO;
        BigDecimal closePrice = BigDecimal.ZERO;
        BigDecimal highPrice = BigDecimal.ZERO;
        BigDecimal lowPrice = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        for (Tick tick : message.getTicks()) {
            String json = tick.toJson();
            ticksStrJoiner.add("\"" + json + "\"");
            ticksJoiner.add(json);
            if (openPrice.compareTo(BigDecimal.ZERO) == 0) {
                openPrice = tick.getPrice();
                closePrice = tick.getPrice();
                highPrice = tick.getPrice();
                lowPrice = tick.getPrice();
            } else {
                closePrice = tick.getPrice();
                highPrice = highPrice.max(tick.getPrice());
                lowPrice = lowPrice.min(tick.getPrice());
            }
            quantity = quantity.add(tick.getQuantity());
        }
        String tickData = ticksJoiner.toString();
        String tickStrData = ticksStrJoiner.toString();
        //更新redis缓存中的tick
        Boolean tickOk = redisService.executeScriptReturnBoolean(this.shaUpdateRecentTicksLua,
                new String[]{RedisKey.RECENT_TICKS.name()},
                new String[]{String.valueOf(this.sequenceId), tickData, tickStrData});
        if (!tickOk.booleanValue()) {
            log.warn("update recent ticks failed, sequenceId: {}", this.sequenceId);
        }
        // 将tick落库
        quotationDbService.saveTicks(message.getTicks());
        // 更新redis缓存中的bar
        long sec = createdAt / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long secStartTime = sec * 1000; // 秒K的开始时间
        long minStartTime = min * 60 * 1000; // 分钟K的开始时间
        long hourStartTime = hour * 3600 * 1000; // 小时K的开始时间
        long dayStartTime = Instant.ofEpochMilli(hourStartTime).atZone(zoneId).withHour(0).toEpochSecond() * 1000; // 日K的开始时间，与TimeZone相关
        String strCreatedBars = redisService.executeScriptReturnString(this.shaUpdateBarLua,
                new String[] { RedisKey.SEC_BARS.name(), RedisKey.MIN_BARS.name(), RedisKey.HOUR_BARS.name(), RedisKey.DAY_BARS.name()},
                new String[] { // ARGV
                        String.valueOf(this.sequenceId), // sequence id
                        String.valueOf(secStartTime), // sec-start-time
                        String.valueOf(minStartTime), // min-start-time
                        String.valueOf(hourStartTime), // hour-start-time
                        String.valueOf(dayStartTime), // day-start-time
                        String.valueOf(openPrice), // open
                        String.valueOf(highPrice), // high
                        String.valueOf(lowPrice), // low
                        String.valueOf(closePrice), // close
                        String.valueOf(quantity) // quantity
                });
        log.info("created bars: {}", strCreatedBars);
        // 将bar(k线图)落库
        Map<BarType, BigDecimal[]> barMap = JsonUtil.readJson(strCreatedBars, typeRef);
        if (barMap.isEmpty()) {
            return;
        }
        DarBar darBar = DarBar.createBar(barMap.get(BarType.DAY));
        HourBar hourBar = HourBar.createBar(barMap.get(BarType.HOUR));
        MinBar minBar = MinBar.createBar(barMap.get(BarType.MIN));
        SecBar secBar = SecBar.createBar(barMap.get(BarType.SEC));
        quotationDbService.saveBars(darBar, hourBar, minBar, secBar);

    }

    private static final TypeReference<Map<BarType, BigDecimal[]>> typeRef = new TypeReference<>() {
    };
}
