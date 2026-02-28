package com.deharri.jlds.listing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListingListResponse {
    private List<JobListingSummaryResponse> listings;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
