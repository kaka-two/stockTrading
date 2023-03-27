package com.kakas.stockTrading.service;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.OrderStatus;
import com.kakas.stockTrading.enums.TransferType;
import com.kakas.stockTrading.pojo.MatchRecord;
import com.kakas.stockTrading.pojo.MatchResult;
import com.kakas.stockTrading.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ClearService {
    private final AssertService assertService;
    private final OrderOpeService orderOpeService;

    public ClearService(@Autowired AssertService assertService, @Autowired OrderOpeService orderOpeService) {
        this.assertService = assertService;
        this.orderOpeService = orderOpeService;
    }

    // 清算交易
    public void clearMatchResult(MatchResult matchResult) {
        Order takerOrder = matchResult.getTakerOrder();
        List<MatchRecord> records = matchResult.getRecords();
        // 根据买和卖选择不同的清算逻辑
        switch (takerOrder.getDirection()) {
            case BUY -> clearMatchResultForBuy(takerOrder, records);
            case SELL -> clearMatchResultForSell(takerOrder, records);
        }
        // 删除交易完的taker
        if (takerOrder.getUnfilledQuantity().signum() == 0) {
            orderOpeService.removeOrder(takerOrder.getOrderId());
        }
    }

    private void clearMatchResultForBuy(Order takerOrder, List<MatchRecord> records) {
        for (MatchRecord record : records) {
            Order makerOrder = record.makerOrder();
            // 获取实际交易额
            BigDecimal transferMoney = record.price().multiply(record.quantity());
            // 金钱转账
            assertService.transfer(takerOrder.getUserId(), makerOrder.getUserId(), AssertType.Money,
                    TransferType.FROZEN_TO_AVAILABLE, transferMoney);
            // 股票转账
            assertService.transfer(makerOrder.getUserId(), takerOrder.getUserId(), AssertType.StockA,
                    TransferType.FROZEN_TO_AVAILABLE, record.quantity());
            // 如果taker的价格大于maker，则将多余的冻结的钱返回账户
            if (takerOrder.getPrice().compareTo(makerOrder.getPrice()) > 0) {
                BigDecimal takerFreezeMoney = takerOrder.getPrice().multiply(record.quantity());
                assertService.unfreeze(takerOrder.getUserId(), AssertType.Money,
                        takerFreezeMoney.subtract(transferMoney));
            }
            // 如果maker已经交易完，删除maker
            if (makerOrder.getOrderStatus() == OrderStatus.FULLY_FILLED) {
                orderOpeService.removeOrder(makerOrder.getOrderId());
            }
        }
    }

    private void clearMatchResultForSell(Order takerOrder, List<MatchRecord> records) {
        for (MatchRecord record : records) {
            Order makerOrder = record.makerOrder();
            // 获取实际交易额
            BigDecimal transferMoney = record.price().multiply(record.quantity());
            // 金钱转账
            assertService.transfer(makerOrder.getUserId(), takerOrder.getUserId(), AssertType.Money,
                    TransferType.FROZEN_TO_AVAILABLE, transferMoney);
            // 股票转账
            assertService.transfer(takerOrder.getUserId(), makerOrder.getUserId(), AssertType.StockA,
                    TransferType.FROZEN_TO_AVAILABLE, record.quantity());
            // 如果maker已经交易完，删除maker
            if (makerOrder.getOrderStatus() == OrderStatus.FULLY_FILLED) {
                orderOpeService.removeOrder(makerOrder.getOrderId());
            }
        }
    }

    public void clearCancelOrder(Order order) {
        // 将冻结的金钱或股票返回账户
        switch (order.getDirection()) {
            case BUY -> {
                BigDecimal orderFreezeMoney = order.getPrice().multiply(order.getUnfilledQuantity());
                assertService.unfreeze(order.getUserId(), AssertType.Money, orderFreezeMoney);
            }
            case SELL -> {
                assertService.unfreeze(order.getUserId(), AssertType.StockA, order.getUnfilledQuantity());
            }
        }
        // 删除订单
        orderOpeService.removeOrder(order.getOrderId());
    }
}
