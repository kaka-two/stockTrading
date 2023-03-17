package com.kakas.stockTrading.messaging;


import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.MessageConverter;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

class KafkaListenerEndpointAdapter implements KafkaListenerEndpoint {

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getGroupId() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public Collection<String> getTopics() {
        return List.of();
    }

    @Override
    public Pattern getTopicPattern() {
        return null;
    }

    @Override
    public String getClientIdPrefix() {
        return null;
    }

    @Override
    public Integer getConcurrency() {
        return Integer.valueOf(1);
    }

    @Override
    public Boolean getAutoStartup() {
        return Boolean.FALSE;
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer listenerContainer, MessageConverter messageConverter) {
    }

    @Override
    public TopicPartitionOffset[] getTopicPartitionsToAssign() {
        return null;
    }

    @Override
    public boolean isSplitIterables() {
        return false;
    }
}

