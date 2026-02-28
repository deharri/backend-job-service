package com.deharri.jlds.worker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProfileListResponse {
    private List<WorkerProfileSummaryResponse> workers;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
