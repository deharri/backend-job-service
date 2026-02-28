package com.deharri.jlds.listing.dto.request;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.UrgencyLevel;
import com.deharri.jlds.enums.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchFilter {
    private WorkerType workerType;
    private PakistanCity city;
    private String area;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private UrgencyLevel urgency;
    @Builder.Default
    private JobStatus status = JobStatus.OPEN;
    private String tags;
    private String search;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDir = "DESC";
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
