package com.deharri.jlds.bid;

import com.deharri.jlds.bid.entity.JobBid;
import com.deharri.jlds.enums.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<JobBid, UUID> {

    Page<JobBid> findByJobIdOrderByCreatedAtDesc(UUID jobId, Pageable pageable);

    Page<JobBid> findByWorkerIdOrderByCreatedAtDesc(UUID workerId, Pageable pageable);

    Page<JobBid> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId, Pageable pageable);

    Optional<JobBid> findByJobIdAndWorkerId(UUID jobId, UUID workerId);

    boolean existsByJobIdAndWorkerId(UUID jobId, UUID workerId);

    boolean existsByJobIdAndAgencyId(UUID jobId, UUID agencyId);

    @Modifying
    @Query("UPDATE JobBid b SET b.status = :status WHERE b.jobId = :jobId AND b.status = 'PENDING' AND b.bidId <> :excludeBidId")
    int rejectOtherPendingBids(@Param("jobId") UUID jobId, @Param("excludeBidId") UUID excludeBidId, @Param("status") BidStatus status);

    long countByJobIdAndStatus(UUID jobId, BidStatus status);

    // ── Analytics ────────────────────────────────────────────────────

    @Query(value =
            "SELECT status, COUNT(*) AS cnt FROM job_bids " +
            "WHERE agency_id = :agencyId " +
            "  AND created_at >= :fromTs AND created_at < :toTs " +
            "GROUP BY status",
            nativeQuery = true)
    List<Object[]> aggAgencyBidStatusCounts(
            @Param("agencyId") UUID agencyId,
            @Param("fromTs") java.time.Instant fromTs,
            @Param("toTs") java.time.Instant toTs);

    /** Bid count for the funnel — "bids placed" stage. */
    @Query("SELECT COUNT(b) FROM JobBid b WHERE b.agencyId = :agencyId")
    long countAllByAgencyId(@Param("agencyId") UUID agencyId);
}
