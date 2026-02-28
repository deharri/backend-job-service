package com.deharri.jlds.worker.dto.request;

import com.deharri.jlds.enums.Language;
import com.deharri.jlds.enums.PakistanCity;
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
public class WorkerSearchFilter {
    private WorkerType workerType;
    private PakistanCity city;
    private PakistanCity serviceCity;
    private String skills;
    private String search;
    private BigDecimal minRating;
    private BigDecimal maxHourlyRate;
    private BigDecimal maxDailyRate;
    private Integer minExperience;
    private Boolean available;
    private Boolean verified;
    private Language language;
    @Builder.Default
    private String sortBy = "rating";
    @Builder.Default
    private String sortDir = "DESC";
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
