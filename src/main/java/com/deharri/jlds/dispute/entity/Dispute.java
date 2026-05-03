package com.deharri.jlds.dispute.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute", indexes = {
        @Index(name = "idx_dispute_raised_by_created", columnList = "raisedBy, createdAt DESC"),
        @Index(name = "idx_dispute_job_id", columnList = "jobId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    private UUID disputeId;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID raisedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RaisedByRole raisedByRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DisputeCategory category;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DisputeStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        if (disputeId == null) {
            disputeId = UUID.randomUUID();
        }
        if (status == null) {
            status = DisputeStatus.OPEN;
        }
    }

    public enum RaisedByRole { CONSUMER, WORKER }

    public enum DisputeCategory { PAYMENT, NO_SHOW, QUALITY, BEHAVIOR, OTHER }

    public enum DisputeStatus { OPEN, UNDER_REVIEW, RESOLVED, CLOSED }
}
