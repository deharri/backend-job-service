package com.deharri.jlds.worker.dto.response;

import com.deharri.jlds.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileResponse {
    private UUID workerId;
    private UUID userId;
    private String username;
    private String firstName;
    private String lastName;
    private String profilePicturePath;
    private WorkerType workerType;
    private String workerTypeDisplayName;
    private List<String> skills;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private PakistanCity city;
    private String cityDisplayName;
    private String area;
    private List<PakistanCity> serviceCities;
    private List<Language> languages;
    private AvailabilityStatus availabilityStatus;
    private Boolean isVerified;
    private BigDecimal averageRating;
    private Integer totalJobsCompleted;
    private String agencyName;
    private Instant lastSyncedAt;
}
