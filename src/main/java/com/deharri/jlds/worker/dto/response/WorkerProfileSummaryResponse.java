package com.deharri.jlds.worker.dto.response;

import com.deharri.jlds.enums.AvailabilityStatus;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileSummaryResponse {
    private UUID workerId;
    private String username;
    private String firstName;
    private String lastName;
    private String profilePicturePath;
    private WorkerType workerType;
    private String workerTypeDisplayName;
    private PakistanCity city;
    private String cityDisplayName;
    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private Integer experienceYears;
    private AvailabilityStatus availabilityStatus;
    private Boolean isVerified;
    private BigDecimal averageRating;
    private Integer totalJobsCompleted;
    private List<String> skills;
}
