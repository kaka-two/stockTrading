package com.kakas.stockTrading.service;

import com.kakas.stockTrading.dbService.EventDetailServiceImpl;
import com.kakas.stockTrading.dbService.EventUniqueServiceImpl;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.messaging.MessageType;
import com.kakas.stockTrading.pojo.EventDetail;
import com.kakas.stockTrading.pojo.EventUnique;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class SequenceHandler {
    @Autowired
    private EventUniqueServiceImpl eventUniqueService;

    @Autowired
    private EventDetailServiceImpl eventDetailService;

    @Autowired
    private MessageType messageType;

    // 上一次处理事件的时间戳
    private long lastTimestamp = 0;
    Set<String> uniqueKeys = null;

    public List<Event> handleEvents(MessageType messageType, AtomicLong sequence, List<Event> events) {
        long t = System.currentTimeMillis();
        if (t < lastTimestamp) {
            log.warn("当前时间" + t + "小于上一次处理事件的时间" + lastTimestamp);
        }
        lastTimestamp = t;
        List<Event> sequencedEvents = null;
        // 防止同一批事件中出现重复事件
        List<EventUnique> uniques = null;
        List<EventDetail> eventDetails = null;
        for (Event event : events) {
            String uniqueId = event.getUniqueId();
            EventUnique unique = null;
            // 假如要处理的事件有uniqueId(例如转账类型的事件，至多只可处理一次)，那么就要检查是否已经处理过了
            if (uniqueId != null) {
                if (eventUniqueService.getEventUnique(uniqueId) != null) {
                    log.warn("忽略已经处理过的事件" + event);
                    continue;
                }
                uniqueKeys.add(uniqueId.intern());
                unique = new EventUnique();
                unique.setUniqueId(uniqueId);
                unique.setCreatedAt(event.getCreatedAt());
                if (uniques == null) {
                    uniques = new ArrayList<>();
                }
                uniques.add(unique);
            }
            // 为事件设置sequence并保存到数组中
            Long previousSequence = sequence.get();
            Long currentSequence = sequence.incrementAndGet();
            event.setSequenceId(currentSequence);
            event.setPreviousId(previousSequence);
            if (sequencedEvents == null) {
                sequencedEvents = new ArrayList<>();
            }
            sequencedEvents.add(event);
            // 如果时间存在uniqueId，将事件与EventUnique进行关联
            if (uniqueId != null) {
                unique.setSequenceId(currentSequence);
            }
            // 将事件的详细信息保存到数组中
            EventDetail eventDetail = EventDetail.create(event, messageType.serialize(event));
            if (eventDetails == null) {
                eventDetails = new ArrayList<>();
            }
            eventDetails.add(eventDetail);
        }
        // 将事件的详细信息保存到数据库中
        if (eventDetails != null) {
            eventDetailService.saveBatch(eventDetails);
        }
        if (uniques != null) {
            eventUniqueService.saveBatch(uniques);
        }
        return sequencedEvents;
    }

}
