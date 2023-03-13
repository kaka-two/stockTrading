package com.kakas.stockTrading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kakas.stockTrading.mapper")
public class TradingEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradingEngineApplication.class, args);
    }
}
