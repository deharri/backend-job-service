package com.deharri.jlds.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponse {
    private List<ReviewResponse> reviews;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
