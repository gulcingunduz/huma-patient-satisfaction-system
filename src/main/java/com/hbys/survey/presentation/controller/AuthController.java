package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.auth.AuthLoginRequest;
import com.hbys.survey.application.dto.auth.AuthSessionResponse;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final StaffUserRepository staffUserRepository;
    private final SessionRegistry sessionRegistry;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            StaffUserRepository staffUserRepository,
            SessionRegistry sessionRegistry
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.staffUserRepository = staffUserRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/csrf")
    public CsrfToken csrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public AuthSessionResponse login(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        HttpSession existingSession = servletRequest.getSession(false);
        if (existingSession != null) {
            sessionRegistry.removeSessionInformation(existingSession.getId());
            servletRequest.changeSessionId();
        }
        HttpSession activeSession = servletRequest.getSession(true);

        // Enforce single active session per account: expire any prior session for this user
        List<SessionInformation> priorSessions = sessionRegistry.getAllSessions(authentication.getPrincipal(), false);
        for (SessionInformation prior : priorSessions) {
            if (!prior.getSessionId().equals(activeSession.getId())) {
                prior.expireNow();
            }
        }
        sessionRegistry.registerNewSession(activeSession.getId(), authentication.getPrincipal());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return findSessionResponse(authentication.getName());
    }

    @GetMapping("/me")
    public AuthSessionResponse currentUser(Authentication authentication) {
        return findSessionResponse(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private AuthSessionResponse findSessionResponse(String username) {
        StaffUser staffUser = staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Active staff user was not found."));
        return new AuthSessionResponse(
                staffUser.getId(),
                staffUser.getUsername(),
                staffUser.getFullName(),
                staffUser.getRole().name()
        );
    }
}
