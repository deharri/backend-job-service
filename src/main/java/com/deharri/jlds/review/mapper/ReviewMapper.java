package com.deharri.jlds.review.mapper;

import com.deharri.jlds.review.dto.request.CreateReviewRequest;
import com.deharri.jlds.review.dto.response.ReviewResponse;
import com.deharri.jlds.review.entity.JobReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "jobId", ignore = true)
    @Mapping(target = "reviewerId", ignore = true)
    @Mapping(target = "revieweeId", ignore = true)
    @Mapping(target = "reviewType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobReview toEntity(CreateReviewRequest request);

    ReviewResponse toResponse(JobReview entity);
}
