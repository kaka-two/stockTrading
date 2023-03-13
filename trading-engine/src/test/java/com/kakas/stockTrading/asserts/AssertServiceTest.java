package com.kakas.stockTrading.asserts;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.TransferType;
import com.kakas.stockTrading.pojo.Assert;
import com.kakas.stockTrading.service.AssertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AssertServiceTest {
    AssertService service;
    static final Long rootId = 1L;
    static final Long[] usersId = {100L, 200L, 300L};
    BigDecimal[] money = {new BigDecimal(1000), new BigDecimal(2000), new BigDecimal(5000)};
    BigDecimal[] stockA = {new BigDecimal(600), new BigDecimal(100), new BigDecimal(1000)};

    @BeforeEach
    void init() {
        service = new AssertService();
        // 初始化数据
        for (int i = 0; i < usersId.length; i++) {
            service.tryTransfer(rootId, usersId[i], AssertType.Money, TransferType.AVAILABLE_TO_AVAILABLE, money[i], true);
            service.tryTransfer(rootId, usersId[i], AssertType.StockA, TransferType.AVAILABLE_TO_AVAILABLE, stockA[i], true);
        }
    }

    @AfterEach
    void tearDown() {
        verify();
    }

    @ParameterizedTest
    @ValueSource(longs = {200L})
    void tryTransfer(long num) {
        // root -> 100, true
        service.tryTransfer(rootId, usersId[0], AssertType.Money, TransferType.AVAILABLE_TO_AVAILABLE, new BigDecimal(num), true);
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(1200L)) == 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {800L})
    void transfer(long num) {
        // 100 -> 200, true
        service.transfer(usersId[0], usersId[1], AssertType.Money, TransferType.AVAILABLE_TO_AVAILABLE, new BigDecimal(num));
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(200L)) == 0);
        assertTrue(service.getAssert(usersId[1], AssertType.Money).getAvailable().compareTo(new BigDecimal(2800L)) == 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {2200L})
    void transferFalse(Long num) {
        // 100 -> 200, false
        assertFalse(service.transfer(usersId[0], usersId[1], AssertType.Money, TransferType.AVAILABLE_TO_AVAILABLE, new BigDecimal(num)));
    }


    @ParameterizedTest
    @ValueSource(longs = {800L})
    void freeze(long num) {
        service.freeze(usersId[0], AssertType.Money, new BigDecimal(num));
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(1000L - 800L)) == 0);
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getFrozen().compareTo(new BigDecimal(800L)) == 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {1200L})
    void freezeFalse(long num) {
        assertFalse(service.freeze(usersId[0], AssertType.Money, new BigDecimal(num)));
    }

    @ParameterizedTest
    @ValueSource(longs = {200L})
    void unfreeze(long num) {
        service.freeze(usersId[0], AssertType.Money, new BigDecimal(500L));
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(1000L - 500L)) == 0);
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getFrozen().compareTo(new BigDecimal(500L)) == 0);
        service.unfreeze(usersId[0], AssertType.Money, new BigDecimal(num));
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(1000L - 500L + 200L)) == 0);
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getFrozen().compareTo(new BigDecimal(500L - 200L)) == 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {600L})
    void unfreezeFalse(long num) {
        service.freeze(usersId[0], AssertType.Money, new BigDecimal(500L));
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getAvailable().compareTo(new BigDecimal(1000L - 500L)) == 0);
        assertTrue(service.getAssert(usersId[0], AssertType.Money).getFrozen().compareTo(new BigDecimal(500L)) == 0);
        assertFalse(service.unfreeze(usersId[0], AssertType.Money, new BigDecimal(num)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void getAssert(int idIndex) {
        assertTrue(service.getAssert(usersId[idIndex], AssertType.Money).getAvailable().compareTo(money[idIndex]) == 0);
        assertTrue(service.getAssert(usersId[idIndex], AssertType.StockA).getAvailable().compareTo(stockA[idIndex]) == 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void getAsserts(int idIndex) {
        assertTrue(service.getAssert(usersId[idIndex], AssertType.Money).getAvailable().compareTo(money[idIndex]) == 0);
        assertTrue(service.getAssert(usersId[idIndex], AssertType.StockA).getAvailable().compareTo(stockA[idIndex]) == 0);
    }

    void verify() {
        BigDecimal totalMoney = new BigDecimal(0);
        BigDecimal totalStockA = new BigDecimal(0);
        for (int i = 0; i < usersId.length; i++) {
            Assert userMoney = service.getAssert(usersId[i], AssertType.Money);
            totalMoney = totalMoney.add(userMoney.getTotal());
            Assert userStock = service.getAssert(usersId[i], AssertType.StockA);
            totalStockA = totalStockA.add(userStock.getTotal());
        }
        BigDecimal rootMoney = service.getAssert(rootId, AssertType.Money).getAvailable().negate();
        BigDecimal rootStock = service.getAssert(rootId, AssertType.StockA).getAvailable().negate();
        assertTrue(totalMoney.compareTo(rootMoney) == 0, String.format("Expect %s but actual %s.", rootMoney.toString(), totalMoney.toString()));
        assertTrue(totalStockA.compareTo(rootStock) == 0, String.format("Expect %s but actual %s.", rootStock.toString(), totalStockA.toString()));
    }
}