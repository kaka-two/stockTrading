package com.kakas.stockTrading.messaging;

import com.kakas.stockTrading.message.Message;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler <T extends Message>{
    void processMessages(List<T> messages);
}
