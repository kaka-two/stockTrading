package com.kakas.stockTrading.push;

import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.message.NotifyMessage;
import com.kakas.stockTrading.util.JsonUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
        // 创建VertX httpserver
        HttpServer server = vertx.createHttpServer();
        // 创建路由
        Router router = Router.router(vertx);
        // 路由处理请求 GET /notification
        router.get("/notification").handler(requestHandler -> {
            // 获取请求
            HttpServerRequest request = requestHandler.request();
            // 解析userId
            log.info("start parse userId from token");
            Supplier<Long> userIdSupplier = () -> {
                String tokenStr = request.getParam("token");
                if (tokenStr != null && tokenStr.length() > 0) {
                    AuthToken authToken = AuthToken.fromSecureString(tokenStr, hmacKey);
                    if (!authToken.isExpired()) {
                        return authToken.userId();
                    }
                }
                return null;
            };
            final Long userId = userIdSupplier.get();
            log.info("parse userId from token: {}", userId);
            // 升级websocket
            log.info("start upgrade websocket");
            request.toWebSocket(ar -> {
                if (ar.succeeded()) {
                    initWebsocket(ar.result(), userId);
                } else {
                    log.error("websocket upgrade failed", ar.cause());
                }
            });
            log.info("upgrade websocket success");
        });
        // 路由请求处理 GET /actuator/health
        router.get("/actuator/health").respond(
                ctx -> ctx.response().putHeader("Content-Type", "Application/json").end("{\"status\"}:{\"up\"}")
        );
        // 路由请求处理其他请求
        router.get().respond(
                ctx -> ctx.response().setStatusCode(404).setStatusMessage("router not found").end()
        );
        // 启动httpserver并监听端口
        server.requestHandler(router).listen(this.serverPort, result -> {
            if (result.succeeded()) {
                log.info("push verticle start success on port : {}", this.serverPort);
            } else {
                log.error("push verticle start failed", result.cause());
                vertx.close();
                System.exit(1);
            }
        });
    }

    void initWebsocket(ServerWebSocket ws, Long userId) {
        String handlerId = ws.textHandlerID();
        log.info("init websocket, userId: {},  handlerId: {}",userId, handlerId);
        // 添加handler
        ws.textMessageHandler(text -> {
            log.info("receive text message: {}", text);
        });
        ws.exceptionHandler(throwable -> {
            log.error("websocket exception", throwable);
        });
        ws.closeHandler(e -> {
            unsubscribeClient(handlerId);
            unsubscribeUser(handlerId, userId);
            log.info("websocket close");
        });
        // 注册client和user
        subscribeClient(handlerId);
        subscribeUser(handlerId, userId);
        // 发送问候语
        if (userId == null) {
            // {"type:status", "status":"connected", "message":"connected as anonymous user"}
            ws.writeTextMessage("{\"type:status\", \"status\":\"connected\", \"message\":\"connected as anonymous user\"}");

        } else {
            // {"type:status", "status":"connected", "message":"connected as a user"}
            ws.writeTextMessage("{\"type:status\", \"status\":\"connected\", \"message\":\"connected as a user\"}");
        }
    }

    public void broadcast(String text) {
        NotifyMessage message;
        try {
            message = JsonUtil.readJson(text, NotifyMessage.class);
        } catch (Exception e) {
            log.error("parse message failed", e);
            return;
        }
        EventBus eb = vertx.eventBus();
        if (message.getUserId() == null) {
            log.info("broadcast to all user : {}", text);
            // broadcast to all user
            handlersSet.forEach(handlerId -> {
                eb.send(handlerId, text);
            });
        } else {
            log.info("broadcast to specific user : {}, userId : {}", text, message.getUserId());
            // broadcast to specific user
            Set<String> handlers = userToHandlersMap.get(message.getUserId());
            if (handlers == null) {
                return;
            }
            handlersSet.forEach(handlerId -> {
                eb.send(handlerId, text);
            });
        }
    }

    void subscribeClient(String handlerId) {
        handlersSet.add(handlerId);
    }
    void unsubscribeClient(String handlerId) {
        handlersSet.remove(handlerId);
    }

    void subscribeUser(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.put(handlerId, userId);
        Set<String> handlers = userToHandlersMap.getOrDefault(userId, new ConcurrentHashSet<>());
        handlers.add(handlerId);
        userToHandlersMap.put(userId, handlers);
        log.info("subscribe user success, userId: {}, handler: {}", userId, handlerId);
    }

    void unsubscribeUser(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.remove(handlerId);
        Set<String> handlers = userToHandlersMap.get(userId);
        if (handlers != null && handlers.size() > 0) {
            handlers.remove(handlerId);
        }
        if (handlers == null || handlers.size() == 0) {
            userToHandlersMap.remove(userId);
            log.info("clear user success, userId: {}", userId);
        } else {
            userToHandlersMap.put(userId, handlers);
        }
        log.info("unsubscribe user success, userId: {}, handler: {}", userId, handlerId);
    }


}
