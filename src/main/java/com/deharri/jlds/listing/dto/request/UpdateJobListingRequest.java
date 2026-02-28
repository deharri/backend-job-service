package com.deharri.jlds.listing.dto.request;

import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.UrgencyLevel;
import com.deharri.jlds.enums.WorkerType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobListingRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    private WorkerType workerType;

    private List<String> tags;

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;

    private BudgetType budgetType;

    private PakistanCity city;

    @Size(max = 200, message = "Area must not exceed 200 characters")
    private String area;

    private Double latitude;

    private Double longitude;

    private UrgencyLevel urgency;

    private LocalDate preferredStartDate;

    private LocalDate preferredEndDate;

    private List<String> imagePaths;
}
