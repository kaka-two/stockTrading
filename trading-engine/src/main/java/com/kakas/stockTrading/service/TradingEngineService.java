package com.kakas.stockTrading.service;

import com.kakas.stockTrading.bean.OrderBookBean;
import com.kakas.stockTrading.dbService.EventDetailService;
import com.kakas.stockTrading.dbService.MatchDetailServiceImpl;
import com.kakas.stockTrading.dbService.OrderServiceImpl;
import com.kakas.stockTrading.enums.TransferType;
import com.kakas.stockTrading.message.ApiMessage;
import com.kakas.stockTrading.message.NotifyMessage;
import com.kakas.stockTrading.message.TickMessage;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.message.event.OrderCancelEvent;
import com.kakas.stockTrading.message.event.OrderRequestEvent;
import com.kakas.stockTrading.message.event.TransferEvent;
import com.kakas.stockTrading.messaging.*;
import com.kakas.stockTrading.pojo.*;
import com.kakas.stockTrading.redis.RedisKey;
import com.kakas.stockTrading.redis.RedisService;
import com.kakas.stockTrading.redis.RedisTopic;
import com.kakas.stockTrading.util.IpUtil;
import com.kakas.stockTrading.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class TradingEngineService {
    int orderBookDepth = 100;

    @Autowired
    AssertService assertService;

    @Autowired
    OrderOpeService orderOpeService;

    @Autowired
    MatchService matchService;

    @Autowired
    ClearService clearService;

    @Autowired
    RedisService redisService;

    @Autowired
    MessagingFactory messagingFactory;

    @Autowired
    EventDetailService eventDetailService;

    @Autowired
    MatchDetailServiceImpl matchDetailService;

    @Autowired
    OrderServiceImpl orderService;

    // message监听器
    private MessageConsumer consumer;

    // 发送message接口
    private MessageProducer<TickMessage> producer;

    // 引擎处理的最新序列号
    private long lastSequenceId;

    // 最新的OrderBook快照，用于异步通知UI。
    private OrderBookBean latestOrderBookBean;

    private boolean orderBookChanged;

    // 更新订单本的lua脚本的sha
    private String shaUpdateOrderBookLua;

    // 异步发送tick
    private Thread tickThread;

    // 异步返回引擎处理结果
    private Thread apiThread;

    // 异步通知用户相关交易信息
    private Thread notifyThread;

    // 异步保存订单本快照用于UI
    private Thread orderBookThread;

    // 异步将订单信息和交易细节保存到数据库
    private Thread dbThread;

    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();

    private Queue<ApiMessage> apiQueue = new ConcurrentLinkedQueue<>();

    private Queue<NotifyMessage> notifyQueue = new ConcurrentLinkedQueue<>();

    private Queue<List<Order>> closedOrderQueue = new ConcurrentLinkedQueue<>();

    private Queue<List<MatchDetail>> matchDetailQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        // 创建监听者和消费者
        this.consumer = messagingFactory.createBatchMessageConsumer(KafkaTopic.TRADE, IpUtil.getHostId(), this::processMessages);
        this.producer = messagingFactory.createMessageProducer(KafkaTopic.TICK);

        // 预先加载lua脚本
        this.shaUpdateOrderBookLua = redisService.loadScriptFromClassPath("/redis/update_orderbook.lua");

        // 异步启动线程
        this.tickThread = new Thread(this::runTickThread, "async-tick");
        tickThread.start();
        this.apiThread = new Thread(this::runApiThread, "async-api");
        apiThread.start();
        this.notifyThread = new Thread(this::runNotifyThread, "async-notify");
        notifyThread.start();
        this.orderBookThread = new Thread(this::runOrderBookThread, "async-orderBook");
        orderBookThread.start();
        this.dbThread = new Thread(this::runDbThread, "async-db");
        dbThread.start();

    }

    @PreDestroy
    public void shutdown() {
        this.consumer.stop();
        this.tickThread.interrupt();
        this.apiThread.interrupt();
        this.notifyThread.interrupt();
        this.orderBookThread.interrupt();
        this.dbThread.interrupt();
    }

    // 监听器处理message的方法
    public void processMessages(List<Event> messages) {
        this.orderBookChanged = false;
        for (Event message : messages) {
            processEvent(message);
        }
        if (this.orderBookChanged) {
            this.latestOrderBookBean = this.matchService.getOrderBookBean(this.orderBookDepth);
        }
    }

    // 监听器实际处理message的方法
    public void processEvent(Event event) {
        // 重复事件
        if (event.getSequenceId() <= this.lastSequenceId) {
            log.warn("skip duplicate event : {}", event);
            return;
        }
        // 错过事件
        if (event.getPreviousId() > this.lastSequenceId) {
            log.warn("event lost, expect previousId : {}, but actual previousId : {}", this.lastSequenceId, event.getPreviousId());
            List<Event> lostEvents = eventDetailService.loadEvents(this.lastSequenceId);
            if (lostEvents.size() == 0) {
                log.warn("can not load events from database");
                panic();
                return;
            }
            for (Event lostEvent : lostEvents) {
                processEvent(lostEvent);
            }
            return;
        }
        // 未知故障
        if (event.getPreviousId() != this.lastSequenceId) {
            log.warn("event unknown, expect previousId : {}, but actual previousId : {}", this.lastSequenceId, event.getPreviousId());
            panic();
            return;
        }
        // 按种类处理event
        if (event instanceof OrderRequestEvent) {
            doCreateOrder((OrderRequestEvent) event);
        } else if (event instanceof  OrderCancelEvent) {
            doCancelOrder((OrderCancelEvent) event);
        } else if (event instanceof TransferEvent) {
            doTransfer((TransferEvent) event);
        } else {
            log.warn("event type unknown : {}", event);
            panic();
            return;
        }
        this.lastSequenceId = event.getSequenceId();
    }

    // 处理创建订单事件
    public void doCreateOrder(OrderRequestEvent event) {
        Order order = orderOpeService.createOrder(event.getUserId(), event.getSequenceId(), event.getDirection(),
                event.getPrice(), event.getQuantity(), event.getCreatedAt());
        // 创建失败
        if (order == null) {
            log.warn("Failed to create order");
            // 推送调用失败消息
            this.apiQueue.add(ApiMessage.createOrderFailed(event.getRefId(), event.getCreatedAt()));
            return;
        }
        // 创建成功，匹配，清算后推送成功调用消息
        MatchResult matchResult = this.matchService.processOrder(order.getCreateAt(), order);
        this.clearService.clearMatchResult(matchResult);
        this.apiQueue.add(ApiMessage.orderSuccess(event.getRefId(), event.getCreatedAt(), order.copyOrder()));
        this.orderBookChanged = true;
        // 收集notification，将taker的交易结果通知用户
        List<NotifyMessage> notifyMsgs = new ArrayList<>();
        notifyMsgs.add(NotifyMessage.createNotifyMessage(event.getCreatedAt(), "order_matched",
                order.getOrderId(), order.copyOrder()));
        // 收集完成的订单closeOrders,同时生成MatchDetail, Tick, 以及maker的notification
        if (matchResult.getRecords().isEmpty()) {
            return;
        }
        List<Order> closedOrders = new ArrayList<>();
        List<MatchDetail> matchDetails = new ArrayList<>();
        List<Tick> ticks = new ArrayList<>();
        if (matchResult.getTakerOrder().getOrderStatus().isFinalStatus) {
            closedOrders.add(matchResult.getTakerOrder());
        }
        for (MatchRecord record : matchResult.getRecords()) {
            Order maker = record.makerOrder();
            notifyMsgs.add(NotifyMessage.createNotifyMessage(event.getCreatedAt(), "order_matched",
                    maker.getOrderId(), maker.copyOrder()));
            if (maker.getOrderStatus().isFinalStatus) {
                closedOrders.add(maker);
            }
            MatchDetail takerMatchDetail = MatchDetail.createMatchDetail(event.getSequenceId(), event.getCreatedAt(), record, true);
            MatchDetail makerMatchDetail = MatchDetail.createMatchDetail(event.getSequenceId(), event.getCreatedAt(), record, false);
            matchDetails.add(takerMatchDetail);
            matchDetails.add(makerMatchDetail);
            Tick tick = Tick.createTick(event.getSequenceId(), event.getCreatedAt(), record);
            ticks.add(tick);
        }
        // 将matchDetails和closedOrders异步写入数据库
        this.matchDetailQueue.add(matchDetails);
        this.closedOrderQueue.add(closedOrders);
        // 异步发送tick消息和用户notification
        TickMessage tickMessage = TickMessage.createTickMessage(event.getCreatedAt(), event.getSequenceId(), ticks);
        this.tickQueue.add(tickMessage);
        this.notifyQueue.addAll(notifyMsgs);
    }

    // 处理取消订单事件
    public void doCancelOrder(OrderCancelEvent event) {
        Order cancelOrder = this.orderOpeService.getOrder(event.getRefOrderId());
        // 订单不存在或不属于发起者
        if (cancelOrder == null || event.getUserId() != cancelOrder.getUserId()) {
            log.warn("Failed to cancel order");
            // 推送调用失败消息
            this.apiQueue.add(ApiMessage.cancelOrderFailed(event.getRefId(), event.getCreatedAt()));
            return;
        }
        this.matchService.cancel(cancelOrder, event.getCreatedAt());
        this.clearService.clearCancelOrder(cancelOrder);
        // 撮合，清算完成后更新状态并发送调用结果，同时通知用户取消结果。
        this.orderBookChanged = true;
        this.apiQueue.add(ApiMessage.orderSuccess(event.getRefId(), event.getCreatedAt(), cancelOrder.copyOrder()));
        this.notifyQueue.add(NotifyMessage.createNotifyMessage(event.getCreatedAt(), "order_canceled", cancelOrder.getUserId(), cancelOrder.copyOrder()));

    }

    // 处理转账事件
    public boolean doTransfer(TransferEvent event) {
        return this.assertService.tryTransfer(event.getFromUserId(), event.getToUserId(), event.getAssertType(),
                TransferType.AVAILABLE_TO_AVAILABLE, event.getAmount(), event.isRoot());
    }

    // 异步将tick消息发送出去，
    public void runTickThread() {
        log.warn("Run tick thread");
        for (;;) {
            List<TickMessage> tickMsgs = new ArrayList<>();
            // 拉取队列中的message
            for (;;) {
                TickMessage tickMsg = this.tickQueue.poll();
                if (tickMsg == null) {
                    break;
                }
                tickMsgs.add(tickMsg);
                if (tickMsgs.size() >= 1000) {
                    break;
                }
            }
            // 如果没有消息就休眠
            if (tickMsgs.isEmpty()) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    log.warn("TickThread sleep exception : {}", Thread.currentThread().getName());
                    throw new RuntimeException(e);
                }
            }
            // 有消息就发送
            this.producer.sendMessage(tickMsgs);
        }

    }

    // 异步将api调用结果消息发送出去
    public void runApiThread() {
        log.warn("Run apiThread");
        for (;;) {
            ApiMessage apiMsg = this.apiQueue.poll();
            // 无消息就休眠
            if (apiMsg == null) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    log.warn("apiThread sleep exception : {}", Thread.currentThread().getName());
                    throw new RuntimeException(e);
                }
            }
            // 有消息，通过redis发送
            redisService.publish(RedisTopic.TRADING_RESULT.name(), JsonUtil.writeJson(apiMsg));
        }
    }

    // 异步将用户交易细节消息发送出去
    public void runNotifyThread() {
        log.warn("Run notifyThread");
        for (;;) {
            NotifyMessage notifyMsg = this.notifyQueue.poll();
            // 无消息就休眠
            if (notifyMsg == null) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    log.warn("notifyThread sleep exception : {}", Thread.currentThread().getName());
                    throw new RuntimeException(e);
                }
            }
            // 有消息，通过redis发送
            redisService.publish(RedisTopic.NOTIFICATION.name(), JsonUtil.writeJson(notifyMsg));
        }
    }

    // 异步将订单快照存放进redis
    public void runOrderBookThread() {
        log.warn("start update orderBook snapshot to redis");
        // 上一个orderbook快照的sequenceId
        Long lastSequenceId = 0L;
        for (;;) {
            // 获取引用
            OrderBookBean orderBookBean = this.latestOrderBookBean;
            // 如果为空，或者不是最新的，就休眠
            if (orderBookBean == null || orderBookBean.getSequenceId() <= lastSequenceId) {
                try {
                    Thread.sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    log.warn("orderBookThread sleep exception : {}", Thread.currentThread().getName());
                    throw new RuntimeException(e);
                }
            }
            // 用lua脚本更新
            this.redisService.executeScriptReturnBoolean(this.shaUpdateOrderBookLua,
                    // key [order_book]
                    new String[] {RedisKey.ORDER_BOOK.name()},
                    // args [sequenceId, jsonData]
                    new String[] {String.valueOf(orderBookBean.getSequenceId()), JsonUtil.writeJson(orderBookBean)});
            // 更新本地sequenceId
            lastSequenceId = orderBookBean.getSequenceId();
        }
    }

    // 异步将交易细节以及完成的订单存放进数据库
    public void runDbThread() {
        log.warn("start db thread");
        // 如果为空，或者不是最新的，就休眠
        for (;;) {
            try {
                saveMatchDetailToDb();
                saveClosedOrderToDb();
            } catch (InterruptedException e) {
                log.warn("{} was interrupted.", Thread.currentThread().getName());
                break;
            }

        }
    }

    public void saveMatchDetailToDb() throws InterruptedException {
        List<MatchDetail> batch = new ArrayList<>();
        for (;;) {
            List<MatchDetail> matchDetails = matchDetailQueue.poll();
            if (matchDetails == null || matchDetails.isEmpty()) {
                Thread.sleep(1);
                break;
            }
            batch.addAll(matchDetails);
            if (batch.size() >= 1000) {
                break;
            }
        }
        batch.sort(MatchDetail::compareTo);
        matchDetailService.saveBatch(batch);
    }

    public void saveClosedOrderToDb() throws InterruptedException {
        List<Order> batch = new ArrayList<>();
        for (;;) {
            List<Order> orders = closedOrderQueue.poll();
            if (orders == null || orders.isEmpty()) {
                Thread.sleep(1);
                break;
            }
            batch.addAll(orders);
            if (batch.size() >= 1000) {
                break;
            }
        }
        batch.sort(Order::compareTo);
        orderService.saveBatch(batch);
    }

    // 程序终止方法
    private void panic() {
        log.error("This application panic. System exit now;");
        System.exit(1);
    }
}
