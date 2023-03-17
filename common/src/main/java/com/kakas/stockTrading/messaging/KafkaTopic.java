package com.kakas.stockTrading.messaging;

public enum KafkaTopic {
    // to sequence
    SEQUENCE,
    // from/to trading-engine
    TRANSFER,
    // event to trading-engine
    TRADE,
    // to quotation to generate bar
    TICK;
}
