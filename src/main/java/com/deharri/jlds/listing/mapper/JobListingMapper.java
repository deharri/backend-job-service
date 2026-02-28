package com.deharri.jlds.listing.mapper;

import com.deharri.jlds.listing.dto.request.CreateJobListingRequest;
import com.deharri.jlds.listing.dto.response.JobListingResponse;
import com.deharri.jlds.listing.dto.response.JobListingSummaryResponse;
import com.deharri.jlds.listing.entity.JobListing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobListingMapper {

    @Mapping(target = "jobId", ignore = true)
    @Mapping(target = "consumerId", ignore = true)
    @Mapping(target = "consumerUsername", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedWorkerId", ignore = true)
    @Mapping(target = "assignedBidId", ignore = true)
    @Mapping(target = "assignedWorkerUsername", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "bidCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobListing toEntity(CreateJobListingRequest request);

    @Mapping(target = "workerTypeDisplayName", expression = "java(entity.getWorkerType().getDisplayName())")
    @Mapping(target = "cityDisplayName", expression = "java(entity.getCity().getDisplayName())")
    JobListingResponse toResponse(JobListing entity);

    @Mapping(target = "workerTypeDisplayName", expression = "java(entity.getWorkerType().getDisplayName())")
    @Mapping(target = "cityDisplayName", expression = "java(entity.getCity().getDisplayName())")
    JobListingSummaryResponse toSummaryResponse(JobListing entity);
}
