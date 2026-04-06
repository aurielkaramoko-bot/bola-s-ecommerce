package com.bolas.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final int API_LIMIT     = 100;
    private static final int LOGIN_LIMIT   = 5;
    private static final int TRACKING_LIMIT = 20; // anti-énumération numéros de commande

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String ip = clientIp(request);
        long now = Instant.now().getEpochSecond();

        if (uri.startsWith("/api/")) {
            if (isLimited("api:" + ip, now, API_LIMIT)) {
                reject(response);
                return;
            }
        }
        if (uri.startsWith("/tracking")) {
            if (isLimited("tracking:" + ip, now, TRACKING_LIMIT)) {
                reject(response);
                return;
            }
        }
        if ("/admin/login".equals(uri) && "POST".equalsIgnoreCase(request.getMethod())) {
            if (isLimited("login:" + ip, now, LOGIN_LIMIT)) {
                reject(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLimited(String key, long now, int limit) {
        Counter c = counters.computeIfAbsent(key, k -> new Counter(now, 0));
        synchronized (c) {
            if (now >= c.windowStart + WINDOW_SECONDS) {
                c.windowStart = now;
                c.count = 0;
            }
            c.count++;
            return c.count > limit;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"too_many_requests\",\"status\":429}");
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // On prend la dernière IP (ajoutée par le proxy de confiance),
            // pas la première qui peut être falsifiée par le client
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Counter {
        long windowStart;
        int count;

        Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

