package com.deharri.jlds.listing.dto.response;

import com.deharri.jlds.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListingResponse {
    private UUID jobId;
    private UUID consumerId;
    private String consumerUsername;
    private String consumerFirstName;
    private String consumerLastName;
    private String title;
    private String description;
    private WorkerType workerType;
    private String workerTypeDisplayName;
    private List<String> tags;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BudgetType budgetType;
    private PakistanCity city;
    private String cityDisplayName;
    private String area;
    private Double latitude;
    private Double longitude;
    private UrgencyLevel urgency;
    private LocalDate preferredStartDate;
    private LocalDate preferredEndDate;
    private List<String> imagePaths;
    private JobStatus status;
    private UUID assignedWorkerId;
    private UUID assignedBidId;
    private String assignedWorkerUsername;
    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant expiresAt;
    private Integer bidCount;
    private Integer viewCount;
    private Instant createdAt;
    private Instant updatedAt;
}
