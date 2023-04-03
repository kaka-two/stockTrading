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

    // null if not set.
    private String refId;

    private Long createdAt;

    // null if not set. For SequenceService in Trading-sequencer.
    private String uniqueId;
}
