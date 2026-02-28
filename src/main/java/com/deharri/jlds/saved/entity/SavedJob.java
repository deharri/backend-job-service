package com.deharri.jlds.saved.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_job_user_job", columnNames = {"user_id", "job_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Builder.Default
    private Instant savedAt = Instant.now();
}
