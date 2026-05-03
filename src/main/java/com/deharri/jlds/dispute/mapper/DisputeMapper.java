package com.deharri.jlds.dispute.mapper;

import com.deharri.jlds.dispute.dto.response.DisputeResponse;
import com.deharri.jlds.dispute.entity.Dispute;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    DisputeResponse toResponse(Dispute dispute);

    List<DisputeResponse> toResponseList(List<Dispute> disputes);
}
