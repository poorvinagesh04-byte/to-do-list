package com.securetodo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    private static class RequestCounter {
        long windowStart;
        int count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 1;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Rate limit sensitive endpoints: login, register, password-reset
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register") || uri.startsWith("/api/auth/reset-password")) {
            String clientIp = getClientIp(request);
            long currentTime = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(clientIp, (key, current) -> {
                if (current == null || (currentTime - current.windowStart) > 60000) {
                    return new RequestCounter(currentTime);
                }
                current.count++;
                return current;
            });

            if (counter.count > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again in a minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(",")) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

