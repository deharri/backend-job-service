package com.deharri.jlds.review.dto.response;

import com.deharri.jlds.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID reviewId;
    private UUID jobId;
    private UUID reviewerId;
    private UUID revieweeId;
    private ReviewType reviewType;
    private Integer rating;
    private String comment;
    private Integer qualityRating;
    private Integer punctualityRating;
    private Integer communicationRating;
    private Integer valueRating;
    private Integer reliabilityRating;
    private Instant createdAt;
}
