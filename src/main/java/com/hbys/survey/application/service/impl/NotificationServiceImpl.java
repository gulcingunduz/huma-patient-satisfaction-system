package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.dto.notification.NotificationResponse;
import com.hbys.survey.application.exception.NotificationNotFoundException;
import com.hbys.survey.application.exception.StaffUserNotFoundException;
import com.hbys.survey.application.service.NotificationService;
import com.hbys.survey.domain.entity.Notification;
import com.hbys.survey.domain.entity.StaffUser;
import com.hbys.survey.infrastructure.repository.NotificationRepository;
import com.hbys.survey.infrastructure.repository.StaffUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StaffUserRepository staffUserRepository;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            StaffUserRepository staffUserRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.staffUserRepository = staffUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String username, boolean unreadOnly) {
        StaffUser staffUser = findActiveStaffUser(username);
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByStaffUserAndReadFalseOrderByCreatedAtDesc(staffUser)
                : notificationRepository.findByStaffUserOrderByCreatedAtDesc(staffUser);
        return notifications.stream().map(NotificationServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String username, Long notificationId) {
        StaffUser staffUser = findActiveStaffUser(username);
        Notification notification = notificationRepository.findById(notificationId)
                .filter(item -> item.getStaffUser().getId().equals(staffUser.getId()))
                .orElseThrow(NotificationNotFoundException::new);
        notification.markAsRead();
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(String username) {
        StaffUser staffUser = findActiveStaffUser(username);
        List<Notification> unread = notificationRepository.findByStaffUserAndReadFalseOrderByCreatedAtDesc(staffUser);
        unread.forEach(Notification::markAsRead);
        return unread.size();
    }

    private StaffUser findActiveStaffUser(String username) {
        return staffUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(StaffUserNotFoundException::new);
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getStaffUser().getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
