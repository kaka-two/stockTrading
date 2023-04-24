package com.kakas.stockTrading.push;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PushVerticle extends AbstractVerticle {
    private String hmacKey;
    private int serverPort;

    PushVerticle(String hmacKey, int serverPort) {
        this.hmacKey = hmacKey;
        this.serverPort = serverPort;
    }

    /**
     * All handlers.
     */
    private final Set<String> handlersSet = new ConcurrentHashSet<>(1000);

    /**
     * userId -> set of handlers.
     */
    private final Map<Long, Set<String>> userToHandlersMap = new ConcurrentHashMap<>(1000);

    /**
     * handler -> userId.
     */
    private final Map<String, Long> handlerToUserMap = new ConcurrentHashMap<>(1000);

    @Override
    public void start() {

    }

    void initWebsocket(ServerWebSocket ws, Long userId) {

    }

    public void broadcast(String text) {

    }


}
