package com.kakas.stockTrading.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RedisService {
    final RedisClient redisClient;
    final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;

    public RedisService(@Autowired RedisConfiguration configs) {
        // 利用redisURI获取配置，然后生成redisClient。
        RedisURI redisURI = RedisURI.builder().redis(configs.getRedis_host(), configs.getRedis_port())
                .withPassword(configs.getRedis_password().toCharArray()).withDatabase(configs.getRedis_database()).build();
        this.redisClient = RedisClient.create(redisURI);

        // 利用pool2依赖包的对象池生成一个Redis连接池
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        this.redisConnectionPool = ConnectionPoolSupport.createGenericObjectPool(() -> redisClient.connect(), poolConfig);
    }

    @PreDestroy
    public void shutdown() {
        this.redisConnectionPool.close();
        this.redisClient.shutdown();
    }

    // 从classpath加载lua脚本
    public String loadScriptFromClassPath(String classPathFile) {
        String script;
        // 读取脚本
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(classPathFile), StandardCharsets.UTF_8))) {
            script = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.warn("Classpath file not found : {}", classPathFile);
            throw new RuntimeException(e);
        }
        // 载入脚本并返回sha
        String sha = executeSync(commands -> {
            try {
                // 返回sha
                return commands.scriptLoad(script);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Load script {} from {}", sha, classPathFile);
        return sha;
    }

    // 执行脚本返回boolean
    public Boolean executeScriptReturnBoolean(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.BOOLEAN, keys, values);
        });
    }

    // 执行脚本返回String
    public String executeScriptReturnString(String sha, String[] keys, String[] values) {
        return executeSync(commands -> {
            return commands.evalsha(sha, ScriptOutputType.VALUE, keys, values);
        });
    }

    // 预留的redis发布消息功能
    public void publish(String topic, String data) {
        executeSync(commands -> {
            return commands.publish(topic, data);
        });
    }

    // 预留的redis订阅消息功能
    // 大概思路是，传入频道和listener，然后创建redis listener的过程中，用传入的listener实际处理消息
    public void subscribe(String channel, Consumer<String> listener) {
        // 通过redisClient获取连接
        StatefulRedisPubSubConnection<String, String> conn = this.redisClient.connectPubSub();
        // 连接需要添加listener，传入一个匿名内部类，然后覆写message方法，用传入的实际listener接收消息。
        conn.addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String channel, String message) {
                listener.accept(message);
            }
        });
        // 连接同步执行，并订阅频道。
        conn.sync().subscribe(channel);
    }

    // 输入回调函数执行线程，此处的回调函数持有一个具体的处理方法，这个函数主要是把公共代码的部分抽离出来。
    public <T> T executeSync(SyncCommandCallback<T> callback) {
        try (StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {
            connection.setAutoFlushCommands(true);
            RedisCommands<String, String> commands = connection.sync();
            return callback.doInConnection(commands);
        } catch (Exception e) {
            log.warn("executeSync redis failed");
            throw new RuntimeException(e);
        }
    }
}
