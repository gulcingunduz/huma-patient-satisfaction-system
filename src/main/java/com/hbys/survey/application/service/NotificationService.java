package com.hbys.survey.application.service;

import com.hbys.survey.application.dto.notification.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getMyNotifications(String username, boolean unreadOnly);

    NotificationResponse markAsRead(String username, Long notificationId);

    int markAllAsRead(String username);
}
