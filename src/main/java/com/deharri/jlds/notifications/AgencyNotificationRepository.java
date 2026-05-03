package com.deharri.jlds.notifications;

import com.deharri.jlds.notifications.entity.AgencyNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgencyNotificationRepository extends JpaRepository<AgencyNotification, UUID> {

    Page<AgencyNotification> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM AgencyNotification n WHERE n.agencyId = :agencyId AND n.readAt IS NULL")
    long countUnread(@Param("agencyId") UUID agencyId);

    @Modifying
    @Query("UPDATE AgencyNotification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.notificationId = :id AND n.agencyId = :agencyId AND n.readAt IS NULL")
    int markRead(@Param("id") UUID id, @Param("agencyId") UUID agencyId);

    @Modifying
    @Query("UPDATE AgencyNotification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.agencyId = :agencyId AND n.readAt IS NULL")
    int markAllRead(@Param("agencyId") UUID agencyId);
}
