package com.kakas.stockTrading.service;

import com.kakas.stockTrading.message.event.Event;
import com.kakas.stockTrading.messaging.KafkaTopic;
import com.kakas.stockTrading.messaging.MessageProducer;
import com.kakas.stockTrading.messaging.MessagingFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendEventService {
    @Autowired
    MessagingFactory messagingFactory;

    private MessageProducer<Event> messageProducer;

    @PostConstruct
    public void init() {
        this.messageProducer = messagingFactory.createMessageProducer(KafkaTopic.SEQUENCE);
    }

    public void sendEvent(Event event) {
        this.messageProducer.sendMessage(event);
    }
}
