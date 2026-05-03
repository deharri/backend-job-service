package com.deharri.jlds.listing.dto.request;

import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.WorkerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobFromOfferRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Worker type is required")
    private WorkerType workerType;

    @NotNull(message = "City is required")
    private PakistanCity city;

    private String area;

    @NotNull(message = "Budget amount is required")
    private BigDecimal budgetAmount;

    private BudgetType budgetType;

    private Integer deliveryDays;

    // Consumer info — populated either way
    @NotNull(message = "Consumer ID is required")
    private UUID consumerId;

    @NotBlank(message = "Consumer username is required")
    private String consumerUsername;

    private String consumerFirstName;

    private String consumerLastName;

    // Worker performer (mutually exclusive with agency performer; service-layer enforced)
    private UUID workerId;
    private String workerUsername;
    private String workerFirstName;
    private String workerLastName;
    private WorkerType workerWorkerType;
    private Integer workerExperienceYears;
    private BigDecimal workerRating;

    // Agency performer (mutually exclusive with worker performer; service-layer enforced)
    private UUID agencyId;
    private String agencyName;
}
