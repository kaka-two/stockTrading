package com.kakas.stockTrading.pojo;

import com.kakas.stockTrading.bean.OrderBookBean;
import com.kakas.stockTrading.bean.OrderBookItemBean;
import com.kakas.stockTrading.enums.Direction;
import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

@Data
public class OrderBook {
    public final Direction direction;
    public final TreeMap<OrderKey, Order> book;

    public OrderBook(Direction direction) {
        this.direction = direction;
        book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    // 添加订单到book中
    public boolean add(Order order) {
        if (order == null) {
            return false;
        }
        book.put(new OrderKey(order.getSequenceId(), order.getPrice()), order);
        return true;
    }

    // 删除book中的订单
    public boolean remove(Order order) {
        return book.remove(new OrderKey(order.getSequenceId(), order.getPrice())) != null;
    }

    // 获取book中的第一个订单，buy则是最高价格，sell则是最低价格
    public Order getFirst() {
        return book.firstEntry().getValue();
    }

    // book的大小
    public int size() {
        return book.size();
    }

    // book中是否存在订单
    public boolean exist(Order order) {
        return book.get(new OrderKey(order.getSequenceId(), order.getPrice())) != null;
    }

    // 买盘价格从高到低排序，如果价格一致就按序列号从低到高排序
    public static final Comparator<OrderKey> SORT_BUY = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            int compare = o2.getPrice().compareTo(o1.getPrice());
            return compare == 0 ? (int) (o1.getSequenceId() - o2.getSequenceId()) : compare;
        }
    };
    // 卖盘价格从低到高排序，如果价格一致就按序列号从低到高排序
    public static final Comparator<OrderKey> SORT_SELL = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            int compare = o1.getPrice().compareTo(o2.getPrice());
            return compare == 0 ? (int) (o1.getSequenceId() - o2.getSequenceId()) : compare;
        }
    };

    // 获取这个OrderBook的快照
    public List<OrderBookItemBean> getOrderBookBean(int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>();
        OrderBookItemBean preItem = null;
        for (OrderKey key : this.book.keySet()) {
            Order order = book.get(key);
            if (preItem == null) {
                preItem = new OrderBookItemBean(order.getPrice(), order.getQuantity());
                items.add(preItem);
                continue;
            }
            if (preItem.getPrice().compareTo(order.getPrice()) == 0) {
                preItem.add(order.getQuantity());
                continue;
            }
            if (items.size() >= maxDepth) {
                break;
            }
            preItem = new OrderBookItemBean(order.getPrice(), order.getQuantity());
            items.add(preItem);
        }
        return items;
    }

}
