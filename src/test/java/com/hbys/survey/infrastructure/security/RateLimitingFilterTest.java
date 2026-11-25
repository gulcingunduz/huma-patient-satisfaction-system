package com.hbys.survey.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitingFilterTest {

    @Test
    void blocksExcessRequestsPerIpAndResetsAfterWindow() throws Exception {
        AtomicReference<Instant> currentInstant = new AtomicReference<>(Instant.parse("2026-10-01T10:00:00Z"));
        Clock mutableClock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentInstant.get();
            }
        };

        RateLimitingFilter filter = new RateLimitingFilter(
                new ObjectMapper().findAndRegisterModules(),
                true,
                2,
                1,
                mutableClock
        );
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/public/surveys/tracking/TRK-1");
            req.setRemoteAddr("10.0.0.15");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, chain);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/api/public/surveys/tracking/TRK-1");
        blockedReq.setRemoteAddr("10.0.0.15");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilterInternal(blockedReq, blockedRes, chain);

        assertThat(blockedRes.getStatus()).isEqualTo(429);
        assertThat(blockedRes.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Advance past 60s window -> allowed again
        currentInstant.set(Instant.parse("2026-10-01T10:01:05Z"));
        MockHttpServletRequest afterWindowReq = new MockHttpServletRequest("GET", "/api/public/surveys/tracking/TRK-1");
        afterWindowReq.setRemoteAddr("10.0.0.15");
        MockHttpServletResponse afterWindowRes = new MockHttpServletResponse();
        filter.doFilterInternal(afterWindowReq, afterWindowRes, chain);

        assertThat(afterWindowRes.getStatus()).isEqualTo(200);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
