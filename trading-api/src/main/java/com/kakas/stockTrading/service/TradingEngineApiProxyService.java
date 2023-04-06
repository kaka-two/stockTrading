package com.kakas.stockTrading.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TradingEngineApiProxyService {
    @Value("#{TradingConfiguration.apiEndpoints.tradingEngineApi}")
    private String tradingEngineApi;

    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            // 设置连接超时
            .connectTimeout(1, TimeUnit.SECONDS)
            // 设置阅读超时
            .readTimeout(1, TimeUnit.SECONDS)
            // 设置连接池
            .connectionPool(new ConnectionPool(20, 60, TimeUnit.SECONDS))
            // 失败后不重试
            .retryOnConnectionFailure(false).build();

    public String get(String url) throws IOException {
        Request request = new Request.Builder().url(tradingEngineApi + url).header("Accept", "*/*").build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                log.error("Connect to trading_engine error {} : {}",
                        Integer.valueOf(response.code()), tradingEngineApi + url);
            }
            try(ResponseBody body = response.body()) {
                String json = body.string();
                if (json == null || json.isEmpty()) {
                    log.error("Connect to trading_engine successful but empty response : {}", json);
                }
                return json;
            }
        }
    }

}
