package com.deharri.jlds.notifications.dto.response;

import com.deharri.jlds.notifications.entity.AgencyNotification.Kind;

import java.time.Instant;
import java.util.UUID;

public record AgencyNotificationDto(
    UUID notificationId,
    UUID agencyId,
    Kind kind,
    UUID jobId,
    String title,
    String body,
    Instant readAt,
    Instant createdAt
) {}
