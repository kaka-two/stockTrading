package com.kakas.stockTrading.ui.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.client.RestClient;
import com.kakas.stockTrading.ctx.UserContext;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * forward "/api/*" to backend api;
 */
@Component
@Slf4j
public class ProxyFilterRegistrationBean extends FilterRegistrationBean<Filter> {
    @Autowired
    RestClient tradingApiClient;

    @Autowired
    ObjectMapper objectMapper;

    @Value("#{tradingConfiguration.hmacKey}")
    String hmacKey;

    @PostConstruct
    public void init() {
        Filter proxyFilter = new ProxyFilter();
        setFilter(proxyFilter);
        addUrlPatterns("/api/*");
        setName(proxyFilter.getClass().getSimpleName());
        setOrder(200);
    }

    class ProxyFilter implements Filter {
        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            // 转换为http对象
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            // 通过proxyForward转发
            log.info("process {} {}", request.getMethod(), request.getRequestURI());
            Long userId = UserContext.getUserId();
            log.info("userId : {}", userId);
            proxyForward(userId, request, response);
        }

        public void proxyForward(Long userId, HttpServletRequest request, HttpServletResponse response) {
            // 获取 path, autoHeader, query
            String path = request.getRequestURI();
            String authHeader = null;
            if (userId != null) {
                AuthToken token = new AuthToken(userId, System.currentTimeMillis() + 60_000);
                authHeader = "Bearer " + token.toSecureString(hmacKey);
            }
            Map<String, String[]> map = request.getParameterMap();
            Map<String, String> query = getQuery(map);
            String respJson = null;
            try {
                if ("GET".equals(request.getMethod())) {
                    respJson = tradingApiClient.get(String.class, path, authHeader, query);
                } else if ("POST".equals(request.getMethod())) {
                    respJson = tradingApiClient.post(String.class, path, authHeader, readBody(request));
                }
            } catch (IOException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }

            // response
            response.setContentType("application/json;charset=utf-8");
            try {
                PrintWriter pw = response.getWriter();
                pw.write(respJson);
                pw.flush();
            } catch (IOException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }

        }

        public Map<String, String> getQuery(Map<String, String[]> map) {
            if (map == null) {
                return null;
            }
            Map<String, String> query = new HashMap<>();
            map.forEach((key, value) -> {
                query.put(key, value[0]);
            });
            return query;
        }

        public String readBody(HttpServletRequest request) throws IOException {
            String json =  request.getReader().lines().collect(Collectors.joining());
            log.info("jsonBody : " + json );
            return json;
        }

    }


}
