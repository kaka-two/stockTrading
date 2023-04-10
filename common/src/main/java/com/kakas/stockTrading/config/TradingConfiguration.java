package com.kakas.stockTrading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;


@Configuration
@ConfigurationProperties(prefix = "trading.config")
@Data
public class TradingConfiguration {
    private int orderBookDepth;

    private ZoneId timeZone = ZoneId.systemDefault();

    private String hmacKey;

    private ApiEndPoints apiEndPoints;

    @Data
    public static class ApiEndPoints {
        private String tradingApi;

        private String tradingEngineApi;
    }
}
