package com.deharri.jlds.listing;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.listing.entity.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, UUID>, JpaSpecificationExecutor<JobListing> {

    Page<JobListing> findByConsumerIdOrderByCreatedAtDesc(UUID consumerId, Pageable pageable);

    Page<JobListing> findByAssignedWorkerIdOrderByCreatedAtDesc(UUID workerId, Pageable pageable);

    @Query("SELECT j FROM JobListing j WHERE j.assignedWorkerId = :workerId AND j.status = 'COMPLETED'")
    Page<JobListing> findCompletedJobsByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);

    List<JobListing> findByStatusAndExpiresAtBefore(JobStatus status, java.time.Instant now);
}
