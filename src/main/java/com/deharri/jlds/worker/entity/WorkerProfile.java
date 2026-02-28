package com.deharri.jlds.worker.entity;

import com.deharri.jlds.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "worker_profiles", indexes = {
        @Index(name = "idx_wp_type_city_status", columnList = "workerType, city, availabilityStatus"),
        @Index(name = "idx_wp_city", columnList = "city"),
        @Index(name = "idx_wp_type", columnList = "workerType"),
        @Index(name = "idx_wp_rating", columnList = "averageRating DESC")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class WorkerProfile {

    @Id
    @Column(name = "worker_id", updatable = false, nullable = false)
    private UUID workerId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String username;

    private String firstName;

    private String lastName;

    private String profilePicturePath;

    @Enumerated(EnumType.STRING)
    private WorkerType workerType;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "wp_skills", joinColumns = @JoinColumn(name = "worker_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Integer experienceYears;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Enumerated(EnumType.STRING)
    private PakistanCity city;

    private String area;

    @Builder.Default
    @ElementCollection(targetClass = PakistanCity.class)
    @CollectionTable(name = "wp_service_cities", joinColumns = @JoinColumn(name = "worker_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "city")
    private List<PakistanCity> serviceCities = new ArrayList<>();

    @Builder.Default
    @ElementCollection(targetClass = Language.class)
    @CollectionTable(name = "wp_languages", joinColumns = @JoinColumn(name = "worker_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private List<Language> languages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Builder.Default
    private Boolean isVerified = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Builder.Default
    private Integer totalJobsCompleted = 0;

    private String agencyName;

    private Instant lastSyncedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
