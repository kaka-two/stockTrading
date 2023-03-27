package com.kakas.stockTrading.message;

import com.kakas.stockTrading.pojo.Tick;
import lombok.Data;

import java.util.List;

/**
 * 用于生成K线图
 */
@Data
public class TickMessage implements Message{
    // null if not set.
    private String refId;

    private Long createdAt;

    private Long sequenceId;

    private List<Tick> ticks;

    public static TickMessage createTickMessage(Long createdAt, Long sequenceId, List<Tick> ticks) {
        TickMessage tickMessage = new TickMessage();
        tickMessage.createdAt = createdAt;
        tickMessage.sequenceId = sequenceId;
        tickMessage.ticks = ticks;
        return tickMessage;
    }
}
