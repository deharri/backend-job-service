package com.deharri.jlds.notifications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "agency_notification", indexes = {
    @Index(name = "idx_agency_notification_agency_id", columnList = "agency_id"),
    @Index(name = "idx_agency_notification_read_at", columnList = "read_at"),
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyNotification {

    public enum Kind {
        BID_PLACED, DISPUTE_OPENED, JOB_DISPATCHED, JOB_CONFIRMED, JOB_CANCELLED,
        PAYMENT_RELEASED, PAYMENT_REFUNDED
    }

    @Id
    @GeneratedValue
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", length = 500)
    private String body;

    @Column(name = "read_at")
    private Instant readAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
