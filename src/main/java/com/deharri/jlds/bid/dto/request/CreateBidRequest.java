package com.deharri.jlds.bid.dto.request;

import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.WorkerType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBidRequest {

    @NotNull(message = "Proposed amount is required")
    @Positive(message = "Proposed amount must be positive")
    private BigDecimal proposedAmount;

    private BudgetType proposedRateType;

    private String coverMessage;

    private Integer estimatedDays;

    private LocalDate proposedStartDate;

    // Denormalized worker info (from frontend/profile) — used when bidding as a worker
    private String workerFirstName;
    private String workerLastName;
    private WorkerType workerWorkerType;
    private Integer workerExperienceYears;
    private BigDecimal workerRating;

    // Agency bidder fields — when set, the bid is from the agency (not the worker)
    private UUID agencyId;
    private String agencyName;
}
