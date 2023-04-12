package com.kakas.stockTrading.service;

import com.kakas.stockTrading.message.Message;
import com.kakas.stockTrading.message.TickMessage;
import com.kakas.stockTrading.messaging.KafkaTopic;
import com.kakas.stockTrading.messaging.MessageConsumer;
import com.kakas.stockTrading.messaging.MessagingFactory;
import com.kakas.stockTrading.pojo.Tick;
import com.kakas.stockTrading.redis.RedisService;
import com.kakas.stockTrading.util.IpUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.StringJoiner;

@Component
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
        BigDecimal openPrize = BigDecimal.ZERO;
        BigDecimal closePrice = BigDecimal.ZERO;
        BigDecimal highPrice = BigDecimal.ZERO;
        BigDecimal lowPrice = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        for (Tick tick : message.getTicks()) {
            String json = tick.toJson();
            ticksStrJoiner.add("\"" + json + "\"");
            ticksJoiner.add(json);
            if (openPrize.compareTo(BigDecimal.ZERO) == 0) {
                openPrize = tick.getPrice();
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
        //更新redis缓存中的tick

        // 将tick落库

        // 更新redis缓存中的bar

        // 将bar落库
    }
}
