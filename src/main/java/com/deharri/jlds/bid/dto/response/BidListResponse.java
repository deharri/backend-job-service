package com.deharri.jlds.bid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidListResponse {
    private List<BidResponse> bids;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
