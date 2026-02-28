package com.deharri.jlds.bid.dto.request;

import com.deharri.jlds.enums.BudgetType;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBidRequest {

    @Positive(message = "Proposed amount must be positive")
    private BigDecimal proposedAmount;

    private BudgetType proposedRateType;

    private String coverMessage;

    private Integer estimatedDays;

    private LocalDate proposedStartDate;
}
