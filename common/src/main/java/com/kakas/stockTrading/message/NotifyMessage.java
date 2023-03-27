package com.kakas.stockTrading.message;

import lombok.Data;

/**
 * tradingEngine的用户交易信息，需要发送给用户
 */
@Data
public class NotifyMessage implements Message{
    // null if not set.
    private String refId;

    private Long createdAt;

    private String type;

    private Long userId;

    private Object data;

    public static NotifyMessage createNotifyMessage(Long createdAt, String type, Long userId, Object data) {
        NotifyMessage message = new NotifyMessage();
        message.createdAt = createdAt;
        message.type = type;
        message.userId = userId;
        message.data = data;
        return message;
    }
}
