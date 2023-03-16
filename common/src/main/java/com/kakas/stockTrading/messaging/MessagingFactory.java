package com.kakas.stockTrading.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessagingFactory {
    @Autowired
    MessageType messageType;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Autowired
    ConcurrentKafkaListenerContainerFactory listenerContainerFactory;


}
