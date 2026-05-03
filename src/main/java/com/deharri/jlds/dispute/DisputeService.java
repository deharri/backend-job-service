package com.deharri.jlds.dispute;

import com.deharri.jlds.dispute.dto.request.SubmitDisputeRequest;
import com.deharri.jlds.dispute.dto.response.DisputeResponse;
import com.deharri.jlds.dispute.entity.Dispute;
import com.deharri.jlds.dispute.entity.Dispute.RaisedByRole;
import com.deharri.jlds.dispute.mapper.DisputeMapper;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.listing.entity.JobListing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingService jobListingService;
    private final DisputeMapper mapper;
    private final com.deharri.jlds.notifications.AgencyNotificationService agencyNotificationService;

    @Transactional
    public DisputeResponse submitDispute(SubmitDisputeRequest request, UUID raisedBy) {
        JobListing listing = jobListingRepository.findById(request.getJobId())
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + request.getJobId()));

        RaisedByRole role = determineRole(listing, raisedBy);

        Dispute dispute = Dispute.builder()
                .jobId(request.getJobId())
                .raisedBy(raisedBy)
                .raisedByRole(role)
                .category(request.getCategory())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(Dispute.DisputeStatus.OPEN)
                .build();

        dispute = disputeRepository.save(dispute);
        log.info("Dispute {} submitted on job {} by user {} as {}",
                dispute.getDisputeId(), dispute.getJobId(), raisedBy, role);

        if (listing.getAssignedAgencyId() != null) {
            agencyNotificationService.publish(
                    listing.getAssignedAgencyId(),
                    com.deharri.jlds.notifications.entity.AgencyNotification.Kind.DISPUTE_OPENED,
                    listing.getJobId(),
                    "New dispute opened on your agency's job",
                    request.getCategory().name() + ": " + request.getSubject());
        }

        return mapper.toResponse(dispute);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> getMyDisputes(UUID userId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<Dispute> disputes = disputeRepository.findByRaisedByOrderByCreatedAtDesc(userId, pageable);
        return disputes.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(UUID disputeId, UUID userId) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new JobNotFoundException("Dispute not found: " + disputeId));
        if (!d.getRaisedBy().equals(userId)) {
            throw new UnauthorizedAccessException("You did not raise this dispute");
        }
        return mapper.toResponse(d);
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> getMyDisputesForJob(UUID jobId, UUID userId) {
        return mapper.toResponseList(
                disputeRepository.findByJobIdAndRaisedByOrderByCreatedAtDesc(jobId, userId));
    }

    private RaisedByRole determineRole(JobListing listing, UUID raisedBy) {
        if (raisedBy.equals(listing.getConsumerId())) {
            return RaisedByRole.CONSUMER;
        }
        if (raisedBy.equals(listing.getAssignedWorkerId())
                || raisedBy.equals(listing.getDispatchedWorkerId())) {
            return RaisedByRole.WORKER;
        }
        throw new UnauthorizedAccessException(
                "You are not a participant in this job and cannot raise a dispute on it");
    }

    /**
     * Agency owner updates the status of a dispute on one of their jobs.
     */
    @Transactional
    public DisputeResponse updateStatusForAgency(UUID disputeId,
                                                 Dispute.DisputeStatus newStatus,
                                                 UUID callerId) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new JobNotFoundException("Dispute not found: " + disputeId));
        verifyCallerOwnsDisputeAgency(d, callerId);
        d.setStatus(newStatus);
        d = disputeRepository.save(d);
        log.info("Dispute {} status changed to {} by agency-owner {}", disputeId, newStatus, callerId);
        return mapper.toResponse(d);
    }

    /** Append / replace the internal admin-style note on a dispute (visible only to the agency). */
    @Transactional
    public DisputeResponse updateNoteForAgency(UUID disputeId, String note, UUID callerId) {
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new JobNotFoundException("Dispute not found: " + disputeId));
        verifyCallerOwnsDisputeAgency(d, callerId);
        d.setAdminNote(note == null ? null : note.trim());
        d = disputeRepository.save(d);
        return mapper.toResponse(d);
    }

    private void verifyCallerOwnsDisputeAgency(Dispute d, UUID callerId) {
        JobListing listing = jobListingRepository.findById(d.getJobId())
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + d.getJobId()));
        if (listing.getAssignedAgencyId() == null) {
            throw new UnauthorizedAccessException(
                    "Cannot update dispute: the underlying job is not assigned to any agency");
        }
        var info = jobListingService.umsFetchAgencyInfo(listing.getAssignedAgencyId());
        if (info == null || !callerId.equals(info.ownerUserId())) {
            throw new UnauthorizedAccessException("Only the agency owner can update this dispute");
        }
    }

    /**
     * Returns disputes raised on any job assigned to the given agency. Caller must own
     * the agency (verified via UMS lookup). Used by the dashboard's Disputes section.
     */
    @Transactional(readOnly = true)
    public List<DisputeResponse> getDisputesForAgency(UUID agencyId, UUID callerId) {
        var info = jobListingService.umsFetchAgencyInfo(agencyId);
        if (info == null || info.ownerUserId() == null) {
            throw new InvalidOperationException("Agency not found: " + agencyId);
        }
        if (!info.ownerUserId().equals(callerId)) {
            throw new UnauthorizedAccessException("Only the agency owner can view this");
        }

        List<UUID> jobIds = jobListingRepository
                .findByAssignedAgencyIdOrderByCreatedAtDesc(agencyId, Pageable.unpaged())
                .stream()
                .map(JobListing::getJobId)
                .toList();
        if (jobIds.isEmpty()) return List.of();

        return disputeRepository.findByJobIdInOrderByCreatedAtDesc(jobIds).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
