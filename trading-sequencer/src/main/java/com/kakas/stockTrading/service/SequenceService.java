package com.kakas.stockTrading.service;

import com.kakas.stockTrading.dbService.EventDetailService;
import com.kakas.stockTrading.dbService.EventDetailServiceImpl;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.messaging.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class SequenceService {

    @Autowired
    SequenceHandler sequenceHandler;

    @Autowired
    MessagingFactory messagingFactory;

    @Autowired
    MessageType messageType;

    @Autowired
    EventDetailServiceImpl eventDetailService;

    // 全局唯一sequenceId。
    private AtomicLong sequence;

    // 唯一组ID
    private static final String groupId = "SequencerGroup";

    // 工作线程
    Thread jobThread;

    // 运行状态,为了保证listener先于线程结束。
    boolean running;

    private MessageProducer<Event> producer;

    // 初始化线程及启动线程
    @PostConstruct
    public void init() {
        this.jobThread = new Thread(() -> {
            log.warn("start sequence job...");
            log.warn("create message consumer for {}...", getClass().getName());
            MessageConsumer consumer = messagingFactory.createBatchMessageConsumer(KafkaTopic.SEQUENCE,
                    groupId, this::processEvents);
            log.warn("create message producer for {}...", getClass().getName());
            this.producer = messagingFactory.createMessageProducer(KafkaTopic.SEQUENCE);
            // 获取上次的sequenceId
            Event lastEvent = eventDetailService.loadLastEvent();
            this.sequence = lastEvent == null ? new AtomicLong(0) : new AtomicLong(lastEvent.getSequenceId());
            // 保持线程运行
            this.running = true;
            while (this.running){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("jobThread is interrupted");
                    this.running = false;
                    break;
                }
            }
            // 关闭consumer
            consumer.stop();
        });
        this.jobThread.start();
    }

    @PreDestroy
    public void destroy() {
        this.jobThread.interrupt();
        try {
            this.jobThread.join(1000);
        } catch (InterruptedException e) {
            log.error("jobThread interrupted failed");
            throw new RuntimeException(e);
        }
    }

    // 处理事件
    synchronized void processEvents(List<Event> events) {
        if (!this.running) {
            log.warn("SequenceService is not running, panic!");
            panic();
        }
        List<Event> sequencedEvents = sequenceHandler.handleEvents(messageType, sequence, events);
        if (sequencedEvents != null && !sequencedEvents.isEmpty()) {
            producer.sendMessage(sequencedEvents);
        }
    }

    // 程序终止时，调用该方法
    private void panic() {
        log.error("This application panic. System exit now;");
        this.running = false;
        System.exit(1);
    }
}
