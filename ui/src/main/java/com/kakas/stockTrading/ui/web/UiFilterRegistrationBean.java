package com.kakas.stockTrading.ui.web;

import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.dbService.UserProfileServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class UiFilterRegistrationBean extends FilterRegistrationBean<Filter> {
    @Autowired
    CookieService cookieService;

    @Autowired
    UserProfileServiceImpl userProfileService;

    @PostConstruct
    public void init() {
        Filter filter = new UiFilter();
        setFilter(filter);
        addUrlPatterns("/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    class UiFilter implements Filter {
        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String path = request.getRequestURI();
            // charset
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            // parse user
            AuthToken authToken = cookieService.findTokenCookie(request);
            if (authToken != null && authToken.isAboutToExpire()) {
                log.info("refresh token");
                cookieService.setTokenCookie(request, response, authToken.refresh());
            }
            Long userId = authToken == null ? null : authToken.userId();
            try (UserContext userContext = new UserContext(userId)) {
                filterChain.doFilter(request, response);
            }
        }
    }
}
