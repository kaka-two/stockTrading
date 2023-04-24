package com.kakas.stockTrading.push;

import io.vertx.core.Vertx;
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
        log.info("start vertx...");
        this.vertx = Vertx.vertx();
        var push = new PushVerticle(this.hmacKey, this.serverPort);
        vertx.deployVerticle(push);
    }

    @PreDestroy
    public void stopVertx() {
        this.vertx.close();
    }
}
