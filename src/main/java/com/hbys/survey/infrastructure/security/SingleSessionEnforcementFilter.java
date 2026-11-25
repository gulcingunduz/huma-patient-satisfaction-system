package com.hbys.survey.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbys.survey.presentation.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces single concurrent session per staff account.
 * When the same account signs in from another device/browser, prior sessions are marked expired
 * and receive a structured 401 SESSION_EXPIRED JSON response on their next request.
 */
@Component
public class SingleSessionEnforcementFilter extends OncePerRequestFilter {

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public SingleSessionEnforcementFilter(SessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SessionInformation info = sessionRegistry.getSessionInformation(session.getId());
            if (info != null && info.isExpired()) {
                sessionRegistry.removeSessionInformation(session.getId());
                session.invalidate();
                SecurityContextHolder.clearContext();

                String path = request.getRequestURI();
                if (path != null && (path.startsWith("/api/public/") || "/api/auth/csrf".equals(path) || "/api/auth/login".equals(path))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Hesabınıza başka bir cihazdan giriş yapıldığı için bu oturum sonlandırıldı.",
                        path,
                        "SESSION_EXPIRED",
                        null
                ));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
