package com.deharri.jlds.review.entity;

import com.deharri.jlds.enums.ReviewType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_review_type", columnNames = {"job_id", "review_type"})
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class JobReview {

    @Id
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID reviewId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private UUID revieweeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false)
    private ReviewType reviewType;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Consumer → Worker sub-ratings
    private Integer qualityRating;
    private Integer punctualityRating;
    private Integer valueRating;

    // Bidirectional sub-rating
    private Integer communicationRating;

    // Worker → Consumer sub-rating
    private Integer reliabilityRating;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        if (reviewId == null) {
            reviewId = UUID.randomUUID();
        }
    }
}
