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

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<JobBid, UUID> {

    Page<JobBid> findByJobIdOrderByCreatedAtDesc(UUID jobId, Pageable pageable);

    Page<JobBid> findByWorkerIdOrderByCreatedAtDesc(UUID workerId, Pageable pageable);

    Optional<JobBid> findByJobIdAndWorkerId(UUID jobId, UUID workerId);

    boolean existsByJobIdAndWorkerId(UUID jobId, UUID workerId);

    @Modifying
    @Query("UPDATE JobBid b SET b.status = :status WHERE b.jobId = :jobId AND b.status = 'PENDING' AND b.bidId <> :excludeBidId")
    int rejectOtherPendingBids(@Param("jobId") UUID jobId, @Param("excludeBidId") UUID excludeBidId, @Param("status") BidStatus status);

    long countByJobIdAndStatus(UUID jobId, BidStatus status);
}
