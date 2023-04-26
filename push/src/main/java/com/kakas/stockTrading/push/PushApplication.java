package com.kakas.stockTrading.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class) // 禁用数据库自动配置 (无DataSource, JdbcTemplate...)
public class PushApplication {
    public static void main(String[] args) {
        System.setProperty("vertx.disableFileCPResolving", "true");
        SpringApplication app = new SpringApplication(PushApplication.class);
        // 禁用Spring的Web，因为我们使用Vertx:
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
