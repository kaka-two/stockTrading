package com.kakas.stockTrading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;


@Configuration
@ConfigurationProperties(prefix = "trading.config")
@Data
public class TradingConfiguration {
    private int orderBookDepth;

    private String timeZone;

    private String hmacKey;

    private ApiEndPoints apiEndPoints;


    @Bean
    public ZoneId createZoneId() {

        return ZoneId.of((this.timeZone == null || this.timeZone.length() == 0) ? ZoneId.systemDefault().getId() : this.timeZone);
    }

    @Data
    public static class ApiEndPoints {
        private String tradingApi;

        private String tradingEngineApi;
    }
}
