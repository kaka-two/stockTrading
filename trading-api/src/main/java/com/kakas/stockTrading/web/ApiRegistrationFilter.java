package com.kakas.stockTrading.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakas.stockTrading.bean.AuthToken;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.dbService.UserProfileServiceImpl;
import com.kakas.stockTrading.pojo.UserProfile;
import com.kakas.stockTrading.util.SignatureUtil;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Slf4j
@Component
public class ApiRegistrationFilter extends FilterRegistrationBean<Filter> {
    @Autowired
    UserProfileServiceImpl userProfileService;

    @Value("#{TradingConfiguration.hmacKey}")
    String hmacKey;

    @PostConstruct
    public void init() {
        ApiFilter filter = new ApiFilter();
        setFilter(filter);
        addUrlPatterns("/api/*");
        setName(filter.getClass().getSimpleName());
        setOrder(100);
    }

    class ApiFilter implements Filter {
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest request  = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            String path = request.getRequestURI();
            log.info("process api {} {}..." + request.getMethod() + path);
            // set default encoding:
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            // try parse user
            Long userId;
            try {
                userId = parserUser(request);
            } catch (RuntimeException e) {
                sendErrorResponse(response);
                return;
            }
            if (userId == null) {
                chain.doFilter(request, response);
            } else {
                try (UserContext userContext = new UserContext(userId)) {
                    chain.doFilter(request, response);
                }
            }
        }

        private Long parserUser(HttpServletRequest request) {
            String auth = request.getHeader("Authorization");
            if (auth == null) {
                throw new RuntimeException("Invalid Authorization header.");
            }
            return parserUserFromAuth(auth);
        }

        private Long parserUserFromAuth(String auth) {
            // 用于普通用户
            if (auth.startsWith("Basic ")) {
                String base64 = auth.substring(6);
                String[] ss = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8).split(":");
                if (ss.length != 2 || ss[0].isBlank() || ss[1].isBlank()) {
                    throw new RuntimeException("Invalid Authorization header.");
                }
                String email = ss[0];
                String password = ss[1];
                UserProfile user = userProfileService.signIn(email, password);
                if (user == null) {
                    throw new RuntimeException("Invalid email or password.");
                }
                return user.getUserId();
            }
            // 用于内部用户
            if (auth.startsWith("Bearer ")) {
                AuthToken token = AuthToken.fromSecureString(auth.substring(7), hmacKey);
                if (token.isExpired()) {
                    throw new RuntimeException("token is expired");
                }
                return token.userId();
            }
            throw new RuntimeException("Invalid Authorization header.");
        }

        void sendErrorResponse(HttpServletResponse response) throws IOException {
            response.sendError(400);
            response.setContentType("application/json");
            PrintWriter pw = response.getWriter();
            pw.write("Invalid email or password.");
            pw.flush();
        }
    }



}
