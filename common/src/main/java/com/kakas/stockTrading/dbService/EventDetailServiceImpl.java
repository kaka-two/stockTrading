package com.kakas.stockTrading.dbService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.EventDetailMapper;
import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.messaging.MessageType;
import com.kakas.stockTrading.pojo.EventDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EventDetailServiceImpl extends ServiceImpl<EventDetailMapper, EventDetail> {
    @Autowired
    MessageType messageType;

    @Autowired
    EventDetailMapper eventDetailMapper;

    // 从数据库读取lastSequenceId之后的所有事件
    public List<Event> loadEvents(long lastSequenceId) {
        QueryWrapper<EventDetail> eventDetailWrapper = new QueryWrapper<>();
        eventDetailWrapper.gt("sequence_id", lastSequenceId).orderByAsc("sequence_id");
        List<EventDetail> eventDetails = eventDetailMapper.selectList(eventDetailWrapper);
        return eventDetails.stream().map(eventDetail -> (Event)messageType.deserialize(eventDetail.getData()))
                .collect(Collectors.toList());
    }

    // 从数据库读取最新的事件
    public Event loadLastEvent() {
        QueryWrapper<EventDetail> eventDetailWrapper = new QueryWrapper<>();
        eventDetailWrapper.orderByDesc("sequence_id").last("limit 1");
        EventDetail eventDetail = eventDetailMapper.selectOne(eventDetailWrapper);
        if (eventDetail == null) {
            return null;
        }
        return (Event)messageType.deserialize(eventDetail.getData());
    }
}
