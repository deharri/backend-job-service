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

    Page<JobListing> findByAssignedAgencyIdOrderByCreatedAtDesc(UUID agencyId, Pageable pageable);

    Page<JobListing> findByDispatchedWorkerIdOrderByCreatedAtDesc(UUID workerId, Pageable pageable);

    /**
     * Returns jobs the worker (by user-id) is currently working on regardless of route —
     * either directly assigned (worker bid accepted) OR dispatched by their agency.
     * Used by the worker's "My Jobs" view so agency-dispatched assignments aren't invisible.
     */
    @Query("SELECT j FROM JobListing j " +
            "WHERE j.assignedWorkerId = :workerUserId OR j.dispatchedWorkerId = :workerUserId " +
            "ORDER BY j.createdAt DESC")
    Page<JobListing> findMyJobsAsWorker(@Param("workerUserId") UUID workerUserId, Pageable pageable);

    @Query("SELECT j FROM JobListing j WHERE j.assignedWorkerId = :workerId AND j.status = 'COMPLETED'")
    Page<JobListing> findCompletedJobsByWorkerId(@Param("workerId") UUID workerId, Pageable pageable);

    @Query("SELECT COUNT(j) FROM JobListing j WHERE j.assignedWorkerId = :workerId AND j.status = 'COMPLETED' AND j.consumerConfirmedAt IS NOT NULL")
    long countConfirmedCompletedByWorkerId(@Param("workerId") UUID workerId);

    @Query("SELECT COUNT(j) FROM JobListing j WHERE j.assignedAgencyId = :agencyId AND j.status = 'COMPLETED' AND j.consumerConfirmedAt IS NOT NULL")
    long countConfirmedCompletedByAgencyId(@Param("agencyId") UUID agencyId);

    /** Number of completed-and-confirmed jobs the worker did under the given agency. */
    @Query("SELECT COUNT(j) FROM JobListing j " +
            "WHERE j.assignedAgencyId = :agencyId " +
            "AND j.dispatchedWorkerId = :workerUserId " +
            "AND j.status = 'COMPLETED' AND j.consumerConfirmedAt IS NOT NULL")
    long countCompletedByAgencyAndWorker(@Param("agencyId") UUID agencyId,
                                         @Param("workerUserId") UUID workerUserId);

    List<JobListing> findByStatusAndExpiresAtBefore(JobStatus status, java.time.Instant now);

    // ── Analytics aggregates ─────────────────────────────────────────

    @Query(value = "SELECT status, COUNT(*) AS cnt FROM job_listings " +
            "WHERE assigned_agency_id = :agencyId GROUP BY status",
            nativeQuery = true)
    List<Object[]> aggStatusCounts(@Param("agencyId") UUID agencyId);

    /** Time series of jobs created per bucket. {@code granularity} is one of 'day', 'week', 'month'. */
    @Query(value =
            "SELECT date_trunc(:granularity, created_at) AS bucket, COUNT(*) AS cnt " +
            "FROM job_listings " +
            "WHERE assigned_agency_id = :agencyId " +
            "  AND created_at >= :fromTs AND created_at < :toTs " +
            "GROUP BY bucket ORDER BY bucket",
            nativeQuery = true)
    List<Object[]> aggJobsOverTime(
            @Param("agencyId") UUID agencyId,
            @Param("granularity") String granularity,
            @Param("fromTs") java.time.Instant fromTs,
            @Param("toTs") java.time.Instant toTs);

    @Query(value = "SELECT city, COUNT(*) AS cnt FROM job_listings " +
            "WHERE assigned_agency_id = :agencyId " +
            "GROUP BY city ORDER BY cnt DESC",
            nativeQuery = true)
    List<Object[]> aggJobsByCity(@Param("agencyId") UUID agencyId);

    @Query(value = "SELECT worker_type, COUNT(*) AS cnt FROM job_listings " +
            "WHERE assigned_agency_id = :agencyId " +
            "GROUP BY worker_type ORDER BY cnt DESC",
            nativeQuery = true)
    List<Object[]> aggJobsByWorkerType(@Param("agencyId") UUID agencyId);

    @Query(value =
            "SELECT dispatched_worker_id, dispatched_worker_username, COUNT(*) AS cnt " +
            "FROM job_listings " +
            "WHERE assigned_agency_id = :agencyId " +
            "  AND dispatched_worker_id IS NOT NULL " +
            "  AND created_at >= :fromTs AND created_at < :toTs " +
            "GROUP BY dispatched_worker_id, dispatched_worker_username " +
            "ORDER BY cnt DESC",
            nativeQuery = true)
    List<Object[]> aggJobsPerWorker(
            @Param("agencyId") UUID agencyId,
            @Param("fromTs") java.time.Instant fromTs,
            @Param("toTs") java.time.Instant toTs);

    /** Pipeline stage counts. bidsAccepted = jobs where assignedAgencyId is set (which by construction means a bid was accepted). */
    @Query(value =
            "SELECT " +
            "  COUNT(*) FILTER (WHERE TRUE) AS bidsAccepted, " +
            "  COUNT(*) FILTER (WHERE dispatched_worker_id IS NOT NULL) AS dispatched, " +
            "  COUNT(*) FILTER (WHERE status IN ('IN_PROGRESS', 'COMPLETED')) AS inProgress, " +
            "  COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed " +
            "FROM job_listings WHERE assigned_agency_id = :agencyId",
            nativeQuery = true)
    Object aggJobsFunnel(@Param("agencyId") UUID agencyId);
}
