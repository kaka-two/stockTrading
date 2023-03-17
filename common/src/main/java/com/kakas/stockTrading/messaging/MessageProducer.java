package com.kakas.stockTrading.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kakas.stockTrading.message.Message;

import java.util.List;

@FunctionalInterface
public interface MessageProducer<T extends Message> {
    void sendMessage(T message);

    default void sendMessage(List<T> messages) {
        for (T message : messages) {
            sendMessage(message);
        }
    }
}
