package com.deharri.jlds.bid.dto.response;

import com.deharri.jlds.enums.BidStatus;
import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private UUID bidId;
    private UUID jobId;
    /** Set on worker bids; null on agency bids. */
    private UUID workerId;
    private String workerUsername;
    private String workerFirstName;
    private String workerLastName;
    /** Set on agency bids; null on worker bids. */
    private UUID agencyId;
    private String agencyName;
    private WorkerType workerWorkerType;
    private Integer workerExperienceYears;
    private BigDecimal workerRating;
    private BigDecimal proposedAmount;
    private BudgetType proposedRateType;
    private String coverMessage;
    private Integer estimatedDays;
    private LocalDate proposedStartDate;
    private BidStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
