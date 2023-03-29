package com.kakas.stockTrading.service;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.UserType;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.message.event.OrderCancelEvent;
import com.kakas.stockTrading.message.event.OrderRequestEvent;
import com.kakas.stockTrading.message.event.TransferEvent;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TradingEngineServiceTest {

    static final Long USER_A = 11111L;
    static final Long USER_B = 22222L;
    static final Long USER_C = 33333L;
    static final Long USER_D = 44444L;

    static final Long[] USERS = { USER_A, USER_B, USER_C, USER_D };

    private long currentSequenceId = 0;

    @Resource
    TradingEngineService engine;

    @Test
    public void testTradingEngineService() {
        engine.processEvent(depositEvent(USER_A, AssertType.Money, bd("58000")));
        engine.processEvent(depositEvent(USER_B, AssertType.Money, bd("126700")));
        engine.processEvent(depositEvent(USER_C, AssertType.StockA, bd("5.5")));
        engine.processEvent(depositEvent(USER_D, AssertType.StockA, bd("8.6")));

//        engine.validate();

        engine.processEvent(orderRequestEvent(USER_A, Direction.BUY, bd("2207.33"), bd("1.2")));
        engine.processEvent(orderRequestEvent(USER_C, Direction.SELL, bd("2215.6"), bd("0.8")));
        engine.processEvent(orderRequestEvent(USER_C, Direction.SELL, bd("2921.1"), bd("0.3")));

//        engine.validate();

        engine.processEvent(orderRequestEvent(USER_D, Direction.SELL, bd("2206"), bd("0.3")));

//        engine.validate();

        engine.processEvent(orderRequestEvent(USER_B, Direction.BUY, bd("2219.6"), bd("2.4")));

//        engine.validate();

        engine.processEvent(orderCancelEvent(USER_A, 1L));

//        engine.validate();

    }

    @Test
    public void testRandom() {
        var r = new Random(123456789);
        for (Long user : USERS) {
            engine.processEvent(depositEvent(user, AssertType.Money, random(r, 1000_0000, 2000_0000)));
            engine.processEvent(depositEvent(user, AssertType.StockA, random(r, 1000, 2000)));
        }
//        engine.validate();

        int low = 20000;
        int high = 40000;
        for (int i = 0; i < 100; i++) {
            Long user = USERS[i % USERS.length];
            engine.processEvent(orderRequestEvent(user, Direction.BUY, random(r, low, high), random(r, 1, 5)));
//            engine.validate();

            engine.processEvent(orderRequestEvent(user, Direction.SELL, random(r, low, high), random(r, 1, 5)));
//            engine.validate();
        }

        assertEquals("35216.4", engine.matchService.lastPrice.stripTrailingZeros().toPlainString());
    }

    BigDecimal random(Random random, int low, int heigh) {
        int n = random.nextInt(low, heigh);
        int m = random.nextInt(100);
        return new BigDecimal(n + "." + m);
    }


    <T extends Event> T createEvent(Class<T> clazz) {
        T event;
        try {
            event = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        event.setPreviousId(this.currentSequenceId);
        this.currentSequenceId++;
        event.setSequenceId(this.currentSequenceId);
        event.setCreatedAt(LocalDateTime.parse("2022-02-22T22:22:22").atZone(ZoneId.of("Z")).toEpochSecond() * 1000
                + this.currentSequenceId);
        return event;
    }

    TransferEvent depositEvent(Long userId, AssertType assetType, BigDecimal amount) {
        var event = createEvent(TransferEvent.class);
        event.setFromUserId(UserType.ROOT.getUserTypeId());
        event.setToUserId(userId);
        event.setAmount(amount);
        event.setAssertType(assetType);
        event.setRoot(true);
        return event;
    }

    OrderRequestEvent orderRequestEvent(Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        var event = createEvent(OrderRequestEvent.class);
        event.setUserId(userId);
        event.setDirection(direction);
        event.setPrice(price);
        event.setQuantity(quantity);
        return event;
    }

    OrderCancelEvent orderCancelEvent(Long userId, Long orderId) {
        var event = createEvent(OrderCancelEvent.class);
        event.setUserId(userId);
        event.setRefOrderId(orderId);
        return event;
    }

    public BigDecimal bd (String s) {
        return new BigDecimal(s);
    }
}