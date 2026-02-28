package com.deharri.jlds.bid.mapper;

import com.deharri.jlds.bid.dto.request.CreateBidRequest;
import com.deharri.jlds.bid.dto.response.BidResponse;
import com.deharri.jlds.bid.entity.JobBid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BidMapper {

    @Mapping(target = "bidId", ignore = true)
    @Mapping(target = "jobId", ignore = true)
    @Mapping(target = "workerId", ignore = true)
    @Mapping(target = "workerUsername", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobBid toEntity(CreateBidRequest request);

    BidResponse toResponse(JobBid entity);
}
