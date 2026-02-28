package com.deharri.jlds.listing.dto.response;

import com.deharri.jlds.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListingSummaryResponse {
    private UUID jobId;
    private String title;
    private WorkerType workerType;
    private String workerTypeDisplayName;
    private PakistanCity city;
    private String cityDisplayName;
    private String area;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BudgetType budgetType;
    private UrgencyLevel urgency;
    private JobStatus status;
    private Integer bidCount;
    private String consumerUsername;
    private Instant createdAt;
    private Instant expiresAt;
}
