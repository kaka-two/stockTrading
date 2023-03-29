package com.kakas.stockTrading.service;

import com.kakas.stockTrading.bean.OrderBookBean;
import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.OrderStatus;
import com.kakas.stockTrading.pojo.MatchResult;
import com.kakas.stockTrading.pojo.Order;
import com.kakas.stockTrading.pojo.OrderBook;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Data
public class MatchService {
    private final OrderBook buyBook;
    private final OrderBook sellBook;
    private long lastSequenceId;
    public BigDecimal lastPrice;

    public MatchService() {
        this.buyBook = new OrderBook(Direction.BUY);
        this.sellBook = new OrderBook(Direction.SELL);
        this.lastPrice = BigDecimal.ZERO;
    }

    // 处理订单并返回匹配结果
    public MatchResult processOrder(long updateAt, Order takerOrder) {
        return switch (takerOrder.getDirection()) {
            case BUY -> processOrder(updateAt, takerOrder, this.sellBook, this.buyBook);
            case SELL -> processOrder(updateAt, takerOrder, this.buyBook, this.sellBook);
            default -> throw new IllegalArgumentException("Direction not found");
        };
    }

    /**
     *
     * @param takerOrder 要处理的订单
     * @param makerBook 可以与处理订单匹配的订单book
     * @param takerBook 未完全处理的订单放在这个book
     * @return 匹配结果
     */
    public MatchResult processOrder(long updateAt, Order takerOrder, OrderBook makerBook, OrderBook takerBook) {
        this.lastSequenceId = takerOrder.getSequenceId();
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal unFilledQuantityOfTaker = takerOrder.getQuantity();
        // 每次循环都和一个maker订单进行匹配，直到匹配完成或者无法继续匹配
        for(;;) {
            // maker盘不存在
            if (makerBook == null || makerBook.size() == 0) {
                break;
            }
            Order makerOrder = makerBook.getFirst();
            // 无法匹配订单
            if (takerOrder.getDirection() == Direction.BUY && takerOrder.getPrice().compareTo(makerOrder.getPrice()) < 0) {
                break;
            }
            if (takerOrder.getDirection() == Direction.SELL && takerOrder.getPrice().compareTo(makerOrder.getPrice()) > 0) {
                break;
            }
            // 以maker价格成交
            BigDecimal price = makerOrder.getPrice();
            this.lastPrice = price;
            // 成交数量是taker和maker的未交易量的最小值
            BigDecimal filledQuantity = unFilledQuantityOfTaker.min(makerOrder.getUnfilledQuantity());
            // 记录交易
            matchResult.add(makerOrder, filledQuantity, price);
            // 更新maker订单，如果匹配完全则删除于makerBook
            BigDecimal unFilledQuantityOfMaker = makerOrder.getUnfilledQuantity().subtract(filledQuantity);
            if (unFilledQuantityOfMaker.signum() > 0) {
                makerOrder.updateOrder(unFilledQuantityOfMaker, OrderStatus.PARTIAL_FILLED, updateAt);
            } else {
                makerOrder.updateOrder(unFilledQuantityOfMaker, OrderStatus.FULLY_FILLED, updateAt);
                makerBook.remove(makerOrder);
            }
            // 更新taker订单，如果匹配完全则退出循环
            unFilledQuantityOfTaker = unFilledQuantityOfTaker.subtract(filledQuantity);
            if (unFilledQuantityOfTaker.signum() > 0) {
                takerOrder.updateOrder(unFilledQuantityOfTaker, OrderStatus.PARTIAL_FILLED, updateAt);
            } else {
                takerOrder.updateOrder(unFilledQuantityOfTaker, OrderStatus.FULLY_FILLED, updateAt);
                break;
            }
        }
        // 将taker剩余的订单进行挂单
        if (unFilledQuantityOfTaker.signum() > 0) {
            takerOrder.updateOrder(unFilledQuantityOfTaker, OrderStatus.PARTIAL_FILLED, updateAt);
            takerBook.add(takerOrder);
        }
        return matchResult;
    }

    // 取消订单
    public void cancel(Order order, long updateAt) {
        OrderBook book = order.getDirection() == Direction.BUY ? this.buyBook : this.sellBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book.");
        }
        OrderStatus orderStatus = order.getOrderStatus() == OrderStatus.PARTIAL_FILLED ? OrderStatus.PARTIAL_CANCELED : OrderStatus.FULLY_CANCELED;
        order.updateOrder(order.getUnfilledQuantity(), orderStatus, updateAt);
    }

    // 获取MatchService此时的所有消息
    public OrderBookBean getOrderBookBean(int maxDepth) {
        return new OrderBookBean(this.lastSequenceId, this.lastPrice, this.buyBook.getOrderBookBean(maxDepth), this.sellBook.getOrderBookBean(maxDepth));
    }

}
