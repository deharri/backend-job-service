package com.deharri.jlds.worker.mapper;

import com.deharri.jlds.worker.dto.response.WorkerProfileResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileSummaryResponse;
import com.deharri.jlds.worker.entity.WorkerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkerProfileMapper {

    @Mapping(target = "workerTypeDisplayName", expression = "java(entity.getWorkerType() != null ? entity.getWorkerType().getDisplayName() : null)")
    @Mapping(target = "cityDisplayName", expression = "java(entity.getCity() != null ? entity.getCity().getDisplayName() : null)")
    WorkerProfileResponse toResponse(WorkerProfile entity);

    @Mapping(target = "workerTypeDisplayName", expression = "java(entity.getWorkerType() != null ? entity.getWorkerType().getDisplayName() : null)")
    @Mapping(target = "cityDisplayName", expression = "java(entity.getCity() != null ? entity.getCity().getDisplayName() : null)")
    WorkerProfileSummaryResponse toSummaryResponse(WorkerProfile entity);
}
