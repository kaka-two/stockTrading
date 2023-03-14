package com.kakas.stockTrading.service;

import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.OrderStatus;
import com.kakas.stockTrading.pojo.MatchRecord;
import com.kakas.stockTrading.pojo.MatchResult;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.pojo.OrderBook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchServiceTest {
    Long userId = 12345L;
    long sequenceId = 0;
    MatchService matchService;
    List<Order> list;

    @BeforeEach
    void setUp() {
        matchService = new MatchService();
        list = List.of(
                createOrder(Direction.BUY, "12300.21", "1.02"), // 0
                createOrder(Direction.BUY, "12305.39", "0.33"), // 1
                createOrder(Direction.SELL, "12305.39", "0.11"), // 2
                createOrder(Direction.SELL, "12300.01", "0.33"), // 3
                createOrder(Direction.SELL, "12400.00", "0.10"), // 4
                createOrder(Direction.SELL, "12400.00", "0.20"), // 5
                createOrder(Direction.SELL, "12390.00", "0.15"), // 6
                createOrder(Direction.BUY, "12400.01", "0.55"), // 7
                createOrder(Direction.BUY, "12300.00", "0.77") // 8
        );
    }


    @Test
    void processOrder() {
        List<MatchRecord> records = new ArrayList<>();
        for (Order order : list) {
            MatchResult result = matchService.processOrder(order.getCreateAt(), order);
            records.addAll(result.getRecords());
        }
        assertArrayEquals(new MatchRecord[] {
                new MatchRecord(list.get(2), list.get(1), new BigDecimal("0.11"), new BigDecimal("12305.39")), //
                new MatchRecord(list.get(3), list.get(1), new BigDecimal("0.22"), new BigDecimal("12305.39")), //
                new MatchRecord(list.get(3), list.get(0), new BigDecimal("0.11"), new BigDecimal("12300.21")), //
                new MatchRecord(list.get(7), list.get(6), new BigDecimal("0.15"), new BigDecimal("12390.00")), //
                new MatchRecord(list.get(7), list.get(4), new BigDecimal("0.10"), new BigDecimal("12400.00")), //
                new MatchRecord(list.get(7), list.get(5), new BigDecimal("0.20"), new BigDecimal("12400.00")), //
        } , records.toArray(MatchRecord[]::new));
    }

    @Test
    void cancel() {
        for (int i = 0; i < 2; i++) {
            MatchResult result = matchService.processOrder(list.get(i).getCreateAt(), list.get(i));
        }
        matchService.cancel(list.get(0), list.get(0).getCreateAt());
        OrderBook book = matchService.getBuyBook();
        assertTrue(book.getFirst() == list.get(1));
        assertTrue(book.size() == 1);
    }

    public Order createOrder(Direction direction, String price, String quantity) {
        this.sequenceId++;
        Order order = new Order();
        order.setOrderId(this.sequenceId << 4);
        order.setSequenceId(this.sequenceId);
        order.setDirection(direction);
        order.setPrice(new BigDecimal(price));
        order.setQuantity(new BigDecimal(quantity));
        order.setUnfilledQuantity(new BigDecimal(quantity));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setUserId(userId);
        order.setCreateAt(1234567890000L + this.sequenceId);
        order.setUpdateAt(order.getCreateAt());
        return order;
    }
}