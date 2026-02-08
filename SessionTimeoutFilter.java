package com.example.project2.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class SessionTimeoutFilter implements Filter {

    private static final long ABSOLUTE_TIMEOUT_MS = 15 * 60 * 1000L; // 15 минут

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            HttpSession session = http.getSession(false);
            if (session != null) {
                Long created = (Long) session.getAttribute("__created_at");
                long now = System.currentTimeMillis();
                if (created == null) {
                    session.setAttribute("__created_at", now);
                } else if (now - created > ABSOLUTE_TIMEOUT_MS) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}


