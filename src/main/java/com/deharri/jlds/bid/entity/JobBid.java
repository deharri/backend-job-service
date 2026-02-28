package com.deharri.jlds.bid.entity;

import com.deharri.jlds.enums.BidStatus;
import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.WorkerType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "job_bids", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_bid_job_worker", columnNames = {"job_id", "worker_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class JobBid {

    @Id
    @Column(name = "bid_id", updatable = false, nullable = false)
    private UUID bidId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(nullable = false)
    private String workerUsername;

    private String workerFirstName;

    private String workerLastName;

    @Enumerated(EnumType.STRING)
    private WorkerType workerWorkerType;

    private Integer workerExperienceYears;

    @Column(precision = 3, scale = 2)
    private BigDecimal workerRating;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal proposedAmount;

    @Enumerated(EnumType.STRING)
    private BudgetType proposedRateType;

    @Column(columnDefinition = "TEXT")
    private String coverMessage;

    private Integer estimatedDays;

    private LocalDate proposedStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        if (bidId == null) {
            bidId = UUID.randomUUID();
        }
    }
}
