package com.hbys.survey.infrastructure.repository;

import com.hbys.survey.domain.entity.Notification;
import com.hbys.survey.domain.entity.NotificationType;
import com.hbys.survey.domain.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByStaffUserOrderByCreatedAtDesc(StaffUser staffUser);

    List<Notification> findByStaffUserAndReadFalseOrderByCreatedAtDesc(StaffUser staffUser);

    List<Notification> findByStaffUserAndTypeOrderByCreatedAtDesc(StaffUser staffUser, NotificationType type);

    List<Notification> findByReadFalseOrderByCreatedAtAsc();

    @Query("select n from Notification n where n.staffUser = :staffUser and n.type = :type and n.read = :read order by n.createdAt desc")
    List<Notification> findByStaffUserAndTypeAndRead(
            @Param("staffUser") StaffUser staffUser,
            @Param("type") NotificationType type,
            @Param("read") boolean read
    );
}
