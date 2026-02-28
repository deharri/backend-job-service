package com.deharri.jlds.listing.entity;

import com.deharri.jlds.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_listings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class JobListing {

    @Id
    @Column(name = "job_id", updatable = false, nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID consumerId;

    @Column(nullable = false)
    private String consumerUsername;

    private String consumerFirstName;

    private String consumerLastName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerType workerType;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_listing_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    private BudgetType budgetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PakistanCity city;

    @Column(length = 200)
    private String area;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UrgencyLevel urgency = UrgencyLevel.NORMAL;

    private LocalDate preferredStartDate;

    private LocalDate preferredEndDate;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "job_listing_images", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "image_path")
    private List<String> imagePaths = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    private UUID assignedWorkerId;

    private UUID assignedBidId;

    private String assignedWorkerUsername;

    private Instant assignedAt;

    private Instant startedAt;

    private Instant completedAt;

    private Instant cancelledAt;

    private String cancellationReason;

    private Instant expiresAt;

    @Builder.Default
    private Integer bidCount = 0;

    @Builder.Default
    private Integer viewCount = 0;

    @Version
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        if (jobId == null) {
            jobId = UUID.randomUUID();
        }
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(30L * 24 * 60 * 60); // 30 days
        }
    }
}
