package com.kakas.stockTrading.service;

import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.messaging.MessageType;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SequenceHandler {
    // 上一次处理事件的时间戳
    private long lastTimestamp = 0;

    public List<Event> handleEvents(MessageType messageType, AtomicLong sequence, List<Event> events) {

    }

}
