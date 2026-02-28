package com.deharri.jlds.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummary {
    private UUID userId;
    private BigDecimal averageRating;
    private Long totalReviews;
    private BigDecimal averageQualityRating;
    private BigDecimal averagePunctualityRating;
    private BigDecimal averageCommunicationRating;
    private BigDecimal averageValueRating;
    private BigDecimal averageReliabilityRating;
}
