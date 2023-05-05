package com.kakas.stockTrading.ui.web;

import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.util.HttpUtil;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class CookieService {
    public final String TokenCookie = "_stocktoken_";

    @Value("#{TradingConfiguration.hmacKey}")
    String hmacKey;

    @Value("#{TradingConfiguration.tokenTimeout}")
    Duration tokenTimeout;

    public long getExpiredInSeconds() {
        return tokenTimeout.toSeconds();
    }

    @Nullable
    public AuthToken findTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TokenCookie.equals(cookie.getName())) {
                String token = cookie.getValue();
                AuthToken authToken = AuthToken.fromSecureString(token, hmacKey);
                return authToken.isExpired() ? null : authToken;
            }
        }
        return null;
    }

    public void setTokenCookie(HttpServletRequest request, HttpServletResponse response, AuthToken authToken) {
        String token = authToken.toSecureString(hmacKey);
        log.info("[Cookie] start set token cookie" + token);
        Cookie cookie = new Cookie(TokenCookie, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        cookie.setMaxAge(3600);
        if (request.getServerName() != null) {
            cookie.setDomain(request.getServerName());
        }
        response.addCookie(cookie);
    }

    public void deleteTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        log.info("[Cookie] delete token cookie");
        Cookie cookie = new Cookie(TokenCookie, "-deleted-");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        cookie.setMaxAge(0);
        if (request.getServerName() != null) {
            cookie.setDomain(request.getServerName());
        }
        response.addCookie(cookie);
    }

}
