package com.kakas.stockTrading.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RestClient {
    final String endpoint;

    final String host;

    ObjectMapper objectMapper;

    OkHttpClient client;

    @Slf4j
    public static class Builder {
        String scheme;
        String host;
        int port;

        int connectTimeOut = 3;
        int readTimeOut = 3;
        int keepAlive = 30;

        // 校验apiEndpoint
        public Builder(String apiEndpoint) {
            log.info("start build RestClient from endpoint: {}", apiEndpoint);
            try {
                URI uri = new URI(apiEndpoint);
                if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
                    throw new IllegalArgumentException("Invalid api endpoint, " + apiEndpoint);
                }
                if (uri.getPath() == null || uri.getPath().isEmpty()) {
                    throw new IllegalArgumentException("Invalid api endpoint, " + apiEndpoint);
                }
                this.scheme = uri.getScheme();
                this.host = uri.getHost();
                this.port = uri.getPort();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public RestClient build(ObjectMapper objectMapper) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(connectTimeOut, TimeUnit.SECONDS)
                    .readTimeout(readTimeOut, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(0, keepAlive, TimeUnit.SECONDS))
                    .retryOnConnectionFailure(false).build();
            String endpoint = this.scheme + "://" + this.host;
            if (port != -1) {
                endpoint = endpoint + ":" + this.port;
            }
            log.info("build RestClient from endpoint successfully");
            return new RestClient(endpoint, host, objectMapper, client);
        }
    }

    RestClient(String endpoint, String host, ObjectMapper objectMapper, OkHttpClient client) {
        this.endpoint = endpoint;
        this.host = host;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    /**
     * @param clazz 类型
     * @param ref 类型
     * @param method get 或 post
     * @param path 请求url路径,以/开头
     * @param authHeader 请求的认证头
     * @param query 请求的参数
     * @param body 请求的body
     * @return clazz或ref类型的对象
     */
    <T> T request(Class<T> clazz, TypeReference<T> ref, String method, String path, String authHeader,
                  Map<String, String> query, Object body) {
        // query
        String queryString = null;
        if (query != null && !query.isEmpty()) {
            List<String> queryList = new ArrayList<>();
            for (Map.Entry<String, String> entry : query.entrySet()) {
                queryList.add(entry.getKey() + "=" + entry.getValue());
            }
            queryString = String.join("&", queryList);
        }
        // url
        StringBuilder urlBuilder = new StringBuilder().append(this.endpoint).append(path);
        if (queryString != null) {
            urlBuilder.append("?").append(queryString);
        }
        final String url = urlBuilder.toString();
        // json body
        String jsonBody = "";
        if (body != null) {
            try {
                jsonBody = body instanceof String ? (String) jsonBody : objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // request
        Request.Builder builder = new Request.Builder().url(url);
        if (authHeader != null) {
            builder.addHeader("Authorization", authHeader);
        }
        if (method.equals("POST")) {
            builder.post(RequestBody.create(jsonBody, JSON));
        }
        Request request = builder.build();
        return execute(clazz, ref, request);
    }

    <T> T execute(Class<T> clazz, TypeReference<T> ref, Request request) {
        log.info("request : {}", request.url().url());
        try (Response response = this.client.newCall(request).execute();
         ResponseBody body = response.body()) {
            String json = body.string();
            if (response.code() == 200) {
                if ("null".equals(json)) {
                    return null;
                }
                if (clazz == null) {
                    return objectMapper.readValue(json, ref);
                }
                if (clazz == String.class) {
                    return (T) json;
                }
                return objectMapper.readValue(json, clazz);
            } else if (response.code() == 400) {
                log.warn("response 400. error : {}", json);
                throw new Exception("request code error" + response.code());
            } else {
                throw new Exception("request code error" + response.code());
            }
        } catch (Exception e) {
            log.error("request execute error : {}", request.url().url());
            throw new RuntimeException(e);
        }
    }

    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


}
