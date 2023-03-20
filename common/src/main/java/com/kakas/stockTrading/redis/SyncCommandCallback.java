package com.kakas.stockTrading.redis;

import io.lettuce.core.api.sync.RedisCommands;

public interface SyncCommandCallback<T> {
    public T doInConnection(RedisCommands<String, String> commands);
}
