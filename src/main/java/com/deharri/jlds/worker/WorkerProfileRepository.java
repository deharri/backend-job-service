package com.deharri.jlds.worker;

import com.deharri.jlds.worker.entity.WorkerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, UUID>, JpaSpecificationExecutor<WorkerProfile> {

    Optional<WorkerProfile> findByUserId(UUID userId);

    boolean existsByWorkerId(UUID workerId);

    long countByLastSyncedAtBefore(Instant threshold);
}
