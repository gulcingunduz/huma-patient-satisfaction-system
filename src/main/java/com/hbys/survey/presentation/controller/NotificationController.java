package com.hbys.survey.presentation.controller;

import com.hbys.survey.application.dto.notification.NotificationResponse;
import com.hbys.survey.application.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return notificationService.getMyNotifications(authentication.getName(), unreadOnly);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        return notificationService.markAsRead(authentication.getName(), notificationId);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead(Authentication authentication) {
        return Map.of("markedReadCount", notificationService.markAllAsRead(authentication.getName()));
    }
}
