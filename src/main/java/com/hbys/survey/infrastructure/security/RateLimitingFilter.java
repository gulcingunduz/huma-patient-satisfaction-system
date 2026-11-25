package com.hbys.survey.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.presentation.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-IP sliding-window rate limiter for public survey and login endpoints.
 * IP addresses are used strictly in volatile memory for abuse protection and never persisted.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int publicLimitPerMinute;
    private final int loginLimitPerMinute;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${hbys.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${hbys.security.rate-limit.public-requests-per-minute:60}") int publicLimitPerMinute,
            @Value("${hbys.security.rate-limit.login-requests-per-minute:15}") int loginLimitPerMinute
    ) {
        this(objectMapper, enabled, publicLimitPerMinute, loginLimitPerMinute, Clock.systemUTC());
    }

    RateLimitingFilter(
            ObjectMapper objectMapper,
            boolean enabled,
            int publicLimitPerMinute,
            int loginLimitPerMinute,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.publicLimitPerMinute = Math.max(1, publicLimitPerMinute);
        this.loginLimitPerMinute = Math.max(1, loginLimitPerMinute);
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        int limit;
        String bucketPrefix;

        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            limit = loginLimitPerMinute;
            bucketPrefix = "LOGIN:";
        } else if (path != null && path.startsWith("/api/public/")) {
            limit = publicLimitPerMinute;
            bucketPrefix = "PUBLIC:";
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String key = bucketPrefix + clientIp;
        if (!tryAcquire(key, limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Too many requests. Please wait a moment and try again.",
                    path,
                    "RATE_LIMIT_EXCEEDED",
                    null
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    boolean tryAcquire(String key, int limit) {
        if (buckets.size() > MAX_TRACKED_KEYS) {
            buckets.clear();
        }
        long now = clock.millis();
        long cutoff = now - WINDOW_MILLIS;
        Deque<Long> timestamps = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
