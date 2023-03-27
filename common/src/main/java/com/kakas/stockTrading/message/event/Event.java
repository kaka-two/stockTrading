package com.kakas.stockTrading.message.event;

import com.kakas.stockTrading.message.Message;
import lombok.Data;

@Data
public abstract class Event implements Message {
    /**
     * Message id, set after sequenced.
     */
    private long sequenceId;

    /**
     * Previous message sequence id.
     */
    private long previousId;
}
