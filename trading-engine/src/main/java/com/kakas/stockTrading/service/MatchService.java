package com.kakas.stockTrading.service;

import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.pojo.MatchResult;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.pojo.OrderBook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MatchService {
    private final OrderBook buyBook;
    private final OrderBook sellBook;
    private long lastSequenceId;
    public BigDecimal lastPrice;

    public MatchService() {
        buyBook = new OrderBook(Direction.BUY);
        sellBook = new OrderBook(Direction.SELL);
        lastPrice = BigDecimal.ZERO;
    }

    // 处理订单并返回匹配结果
    public MatchResult processOrder(Order takerOrder) {
        switch (takerOrder.getDirection()) {
            case BUY -> {
                return processOrder(takerOrder, sellBook, buyBook);
            }
            case SELL -> {
                return processOrder(takerOrder, buyBook, sellBook);
            }
        }
    }

    /**
     *
     * @param takerOrder 要处理的订单
     * @param makerBook 可以与处理订单匹配的订单book
     * @param takerBook 未完全处理的订单放在这个book
     * @return 匹配结果
     */
    public MatchResult processOrder(Order takerOrder, OrderBook makerBook, OrderBook takerBook) {
        this.lastSequenceId = takerOrder.getSequenceId();
        // ！！！ 这个还存疑
        long updateAt = takerOrder.getCreateAt();
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal unFilledQuantityOfTaker = takerOrder.getQuantity();
        // 每次循环都和一个maker订单进行匹配，直到匹配完成或者无法继续匹配
        for(;;) {
            Order makerOrder = makerBook.getFirst();
            switch (takerOrder.getDirection()) {
                case BUY -> {
                    if (takerOrder.getPrice().compareTo(makerOrder.getPrice()) < 0) {
                        break;
                    }
                }
                case SELL -> {
                    if (takerOrder.getPrice().compareTo(makerOrder.getPrice()) > 0) {
                        break;
                    }
                }
            }
            BigDecimal price = takerOrder.getPrice();
            BigDecimal filledQuantity = takerOrder.getQuantity().min(makerOrder.getQuantity());
            matchResult.add(makerOrder, filledQuantity, price);
            unFilledQuantityOfTaker = unFilledQuantityOfTaker.subtract(filledQuantity);
            //接下来是处理更新订单，将数量为0的订单排除掉，如果有剩余数量，在循环外将taker订单进行挂单。
            takerOrder.updateOrder( );
        }
    }

    // 取消订单
    public void cancel(Order order) {

    }
}
