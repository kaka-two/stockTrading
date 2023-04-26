package com.kakas.stockTrading.push;

import com.kakas.stockTrading.redis.RedisTopic;
import io.vertx.core.Vertx;
import io.vertx.redis.client.*;
import io.vertx.redis.client.impl.types.BulkType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushService {
    @Value("${server.port}")
    private int serverPort;

    @Value("${trading.config.hmac-key}")
    String hmacKey;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.database}")
    private int redisDatabase;

    private Vertx vertx;

    @PostConstruct
    public void startVertx() {
        // 启动vertx
        log.info("start vertx...");
        this.vertx = Vertx.vertx();
        var push = new PushVerticle(this.hmacKey, this.serverPort);
        vertx.deployVerticle(push);
        // 连接到redis
        // redis://:password@hostname:port/db_number
        String url = "redis://" + (this.redisPassword.isEmpty() ? "" : ":" + this.redisPassword + "@") +
                this.redisHost + ":" + this.redisPort + "/" + this.redisDatabase;
        Redis redis = Redis.createClient(vertx, url);
        redis.connect().onSuccess(conn -> {
            log.info("connect to redis ok");
            // 对redis连接接收的每一条消息采取以下处理方式
            conn.handler(response -> {
                // 表示是否是redis publish的消息
                if (response.type() == ResponseType.PUSH) {
                    int size = response.size();
                    // 频道名称、消息计数和消息内容
                    if (size == 3) {
                        Response type = response.get(2);
                        // 如果消息内容是BulkType（redis的批量字符串类型）的实例，就广播出去
                        if (type instanceof BulkType) {
                            String msg = type.toString();
                            log.info("receive push msg : {}",msg);
                            push.broadcast(msg);
                        }
                    }
                }
            });
            log.info("try subscribe Redis notification");
            // 调用redis连接的send函数，并发送命令和参数,订阅notification
            conn.send(Request.cmd(Command.SUBSCRIBE).arg(RedisTopic.NOTIFICATION.name())).onSuccess(response -> {
                log.info("subscribe Redis notification ok");
            }).onFailure(err -> {
                log.error("subscribe Redis notification failed!", err);
                System.exit(1);
            });
        }).onFailure(err -> {
            log.error("connect to redis failed!", err);
            System.exit(1);
        });
    }
    @PreDestroy
    public void stopVertx() {
        this.vertx.close();
        System.exit(1);
    }
}
