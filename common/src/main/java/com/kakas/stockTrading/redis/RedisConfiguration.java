package com.kakas.stockTrading.redis;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
@Configuration
@Data
public class RedisConfiguration {
    @Value("${spring.redis.host}")
    String redis_host;

    @Value("${spring.redis.port}")
    int redis_port;

    @Value("${spring.redis.password}")
    String redis_password;

    @Value("${spring.redis.database}")
    int redis_database;
}
