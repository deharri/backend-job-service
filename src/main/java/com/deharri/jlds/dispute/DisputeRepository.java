package com.deharri.jlds.dispute;

import com.deharri.jlds.dispute.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Page<Dispute> findByRaisedByOrderByCreatedAtDesc(UUID raisedBy, Pageable pageable);

    List<Dispute> findByJobIdAndRaisedByOrderByCreatedAtDesc(UUID jobId, UUID raisedBy);

    /** Disputes on any of the given job IDs, newest first. Used by the agency-disputes view. */
    List<Dispute> findByJobIdInOrderByCreatedAtDesc(List<UUID> jobIds);
}
