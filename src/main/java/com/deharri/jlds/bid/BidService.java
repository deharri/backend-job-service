package com.deharri.jlds.bid;

import com.deharri.jlds.bid.dto.request.CreateBidRequest;
import com.deharri.jlds.bid.dto.request.UpdateBidRequest;
import com.deharri.jlds.bid.dto.response.BidListResponse;
import com.deharri.jlds.bid.dto.response.BidResponse;
import com.deharri.jlds.bid.entity.JobBid;
import com.deharri.jlds.bid.mapper.BidMapper;
import com.deharri.jlds.enums.BidStatus;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.error.exception.BidNotFoundException;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.listing.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final BidMapper bidMapper;
    private final JobListingService jobListingService;
    private final JobListingRepository jobListingRepository;
    private final com.deharri.jlds.notifications.AgencyNotificationService agencyNotificationService;

    @Transactional
    public BidResponse placeBid(UUID jobId, CreateBidRequest request, UUID callerId, String username) {
        JobListing listing = jobListingService.findListingOrThrow(jobId);

        if (listing.getStatus() != JobStatus.OPEN) {
            throw new InvalidOperationException("Can only bid on jobs with OPEN status");
        }

        if (listing.getConsumerId().equals(callerId)) {
            throw new InvalidOperationException("Cannot bid on your own job listing");
        }

        boolean isAgencyBid = request.getAgencyId() != null;

        // Agency-bid validation: caller must be the agency owner AND the agency must have an active subscription.
        if (isAgencyBid) {
            JobListingService.UmsAgencyInfo agencyInfo = jobListingService.umsFetchAgencyInfo(request.getAgencyId());
            if (agencyInfo == null || agencyInfo.ownerUserId() == null) {
                throw new InvalidOperationException("Agency not found: " + request.getAgencyId());
            }
            if (!agencyInfo.ownerUserId().equals(callerId)) {
                throw new UnauthorizedAccessException("Only the agency owner can place a bid on behalf of the agency");
            }
            if (!agencyInfo.subscriptionActive()) {
                throw new InvalidOperationException("Agency subscription is inactive — renew to place bids");
            }
        }

        // Duplicate-bid check per identity
        if (isAgencyBid) {
            if (bidRepository.existsByJobIdAndAgencyId(jobId, request.getAgencyId())) {
                throw new InvalidOperationException("Your agency has already placed a bid on this job");
            }
        } else {
            if (bidRepository.existsByJobIdAndWorkerId(jobId, callerId)) {
                throw new InvalidOperationException("You have already placed a bid on this job");
            }
        }

        JobBid bid = bidMapper.toEntity(request);
        bid.setJobId(jobId);
        bid.setStatus(BidStatus.PENDING);
        if (isAgencyBid) {
            bid.setAgencyId(request.getAgencyId());
            bid.setAgencyName(request.getAgencyName());
            // Worker fields stay null
            bid.setWorkerId(null);
            bid.setWorkerUsername(null);
        } else {
            bid.setWorkerId(callerId);
            bid.setWorkerUsername(username);
        }

        bid = bidRepository.save(bid);

        // Increment bid count
        listing.setBidCount(listing.getBidCount() + 1);
        jobListingRepository.save(listing);

        log.info("Bid placed: {} on job: {} by {}: {}", bid.getBidId(), jobId,
                isAgencyBid ? "agency" : "worker",
                isAgencyBid ? request.getAgencyName() : username);

        if (listing.getAssignedAgencyId() != null) {
            agencyNotificationService.publish(
                    listing.getAssignedAgencyId(),
                    com.deharri.jlds.notifications.entity.AgencyNotification.Kind.BID_PLACED,
                    listing.getJobId(),
                    "New bid on a job assigned to your agency",
                    "Bid by " + (request.getAgencyId() != null ? "an agency" : "a worker"));
        }
        return bidMapper.toResponse(bid);
    }

    @Transactional(readOnly = true)
    public BidListResponse getBidsForJob(UUID jobId, UUID consumerId, int page, int size) {
        JobListing listing = jobListingService.findListingOrThrow(jobId);

        if (!listing.getConsumerId().equals(consumerId)) {
            throw new UnauthorizedAccessException("Only the job owner can view bids");
        }

        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobBid> bids = bidRepository.findByJobIdOrderByCreatedAtDesc(jobId, pageable);

        return BidListResponse.builder()
                .bids(bids.getContent().stream().map(bidMapper::toResponse).toList())
                .page(bids.getNumber())
                .size(bids.getSize())
                .totalElements(bids.getTotalElements())
                .totalPages(bids.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public BidResponse getBidById(UUID bidId, UUID userId) {
        JobBid bid = findBidOrThrow(bidId);
        JobListing listing = jobListingService.findListingOrThrow(bid.getJobId());

        // Only bid owner or job owner can view bid details
        if (!bid.getWorkerId().equals(userId) && !listing.getConsumerId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to view this bid");
        }

        return bidMapper.toResponse(bid);
    }

    @Transactional
    public BidResponse updateBid(UUID bidId, UpdateBidRequest request, UUID workerId) {
        JobBid bid = findBidOrThrow(bidId);

        if (!bid.getWorkerId().equals(workerId)) {
            throw new UnauthorizedAccessException("Only the bid owner can update this bid");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidOperationException("Can only update bids in PENDING status");
        }

        if (request.getProposedAmount() != null) bid.setProposedAmount(request.getProposedAmount());
        if (request.getProposedRateType() != null) bid.setProposedRateType(request.getProposedRateType());
        if (request.getCoverMessage() != null) bid.setCoverMessage(request.getCoverMessage());
        if (request.getEstimatedDays() != null) bid.setEstimatedDays(request.getEstimatedDays());
        if (request.getProposedStartDate() != null) bid.setProposedStartDate(request.getProposedStartDate());

        bid = bidRepository.save(bid);
        return bidMapper.toResponse(bid);
    }

    @Transactional
    public BidResponse withdrawBid(UUID bidId, UUID workerId) {
        JobBid bid = findBidOrThrow(bidId);

        if (!bid.getWorkerId().equals(workerId)) {
            throw new UnauthorizedAccessException("Only the bid owner can withdraw this bid");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidOperationException("Can only withdraw bids in PENDING status");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        bid = bidRepository.save(bid);

        // Decrement bid count
        JobListing listing = jobListingService.findListingOrThrow(bid.getJobId());
        listing.setBidCount(Math.max(0, listing.getBidCount() - 1));
        jobListingRepository.save(listing);

        log.info("Bid withdrawn: {} by worker: {}", bidId, workerId);
        return bidMapper.toResponse(bid);
    }

    @Transactional
    public BidResponse acceptBid(UUID bidId, UUID consumerId) {
        JobBid bid = findBidOrThrow(bidId);
        JobListing listing = jobListingService.findListingOrThrow(bid.getJobId());

        if (!listing.getConsumerId().equals(consumerId)) {
            throw new UnauthorizedAccessException("Only the job owner can accept bids");
        }

        if (listing.getStatus() != JobStatus.OPEN) {
            throw new InvalidOperationException("Can only accept bids on OPEN jobs");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidOperationException("Can only accept bids in PENDING status");
        }

        // Accept this bid
        bid.setStatus(BidStatus.ACCEPTED);
        bid = bidRepository.save(bid);

        // Assign performer to job — branch on bid type
        listing.setStatus(JobStatus.ASSIGNED);
        listing.setAssignedBidId(bid.getBidId());
        listing.setAssignedAt(Instant.now());
        if (bid.getAgencyId() != null) {
            listing.setAssignedAgencyId(bid.getAgencyId());
            listing.setAssignedAgencyName(bid.getAgencyName());
        } else {
            listing.setAssignedWorkerId(bid.getWorkerId());
            listing.setAssignedWorkerUsername(bid.getWorkerUsername());
        }
        jobListingRepository.save(listing);

        // Reject all other pending bids
        int rejected = bidRepository.rejectOtherPendingBids(bid.getJobId(), bid.getBidId(), BidStatus.REJECTED);
        log.info("Bid accepted: {} for job: {}. {} other bids rejected.", bidId, bid.getJobId(), rejected);

        return bidMapper.toResponse(bid);
    }

    @Transactional
    public BidResponse rejectBid(UUID bidId, UUID consumerId) {
        JobBid bid = findBidOrThrow(bidId);
        JobListing listing = jobListingService.findListingOrThrow(bid.getJobId());

        if (!listing.getConsumerId().equals(consumerId)) {
            throw new UnauthorizedAccessException("Only the job owner can reject bids");
        }

        if (bid.getStatus() != BidStatus.PENDING) {
            throw new InvalidOperationException("Can only reject bids in PENDING status");
        }

        bid.setStatus(BidStatus.REJECTED);
        bid = bidRepository.save(bid);
        log.info("Bid rejected: {}", bidId);
        return bidMapper.toResponse(bid);
    }

    @Transactional(readOnly = true)
    public BidListResponse getMyBids(UUID workerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobBid> bids = bidRepository.findByWorkerIdOrderByCreatedAtDesc(workerId, pageable);

        return BidListResponse.builder()
                .bids(bids.getContent().stream().map(bidMapper::toResponse).toList())
                .page(bids.getNumber())
                .size(bids.getSize())
                .totalElements(bids.getTotalElements())
                .totalPages(bids.getTotalPages())
                .build();
    }

    /**
     * Returns bids placed on behalf of an agency. The caller must be the agency owner
     * (verified via UMS lookup), so the FE can safely call this when the user is in
     * agency mode without exposing other agencies' bids.
     */
    @Transactional(readOnly = true)
    public BidListResponse getMyAgencyBids(UUID agencyId, UUID callerId, int page, int size) {
        JobListingService.UmsAgencyInfo info = jobListingService.umsFetchAgencyInfo(agencyId);
        if (info == null || info.ownerUserId() == null) {
            throw new InvalidOperationException("Agency not found: " + agencyId);
        }
        if (!info.ownerUserId().equals(callerId)) {
            throw new UnauthorizedAccessException("Only the agency owner can view this agency's bids");
        }

        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobBid> bids = bidRepository.findByAgencyIdOrderByCreatedAtDesc(agencyId, pageable);

        return BidListResponse.builder()
                .bids(bids.getContent().stream().map(bidMapper::toResponse).toList())
                .page(bids.getNumber())
                .size(bids.getSize())
                .totalElements(bids.getTotalElements())
                .totalPages(bids.getTotalPages())
                .build();
    }

    private JobBid findBidOrThrow(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new BidNotFoundException("Bid not found: " + bidId));
    }

    // ── Analytics ────────────────────────────────────────────────────

    public com.deharri.jlds.bid.dto.response.BidWinRate getAgencyWinRate(
            UUID agencyId, UUID callerId,
            java.time.Instant from, java.time.Instant to) {
        JobListingService.UmsAgencyInfo info = jobListingService.umsFetchAgencyInfo(agencyId);
        if (info == null || !callerId.equals(info.ownerUserId())) {
            throw new UnauthorizedAccessException("Only the agency owner can view this");
        }

        long won = 0, pending = 0, rejected = 0, withdrawn = 0;
        for (Object[] row : bidRepository.aggAgencyBidStatusCounts(agencyId, from, to)) {
            String status = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            switch (status) {
                case "ACCEPTED": won = cnt; break;
                case "PENDING": pending = cnt; break;
                case "REJECTED": rejected = cnt; break;
                case "WITHDRAWN": withdrawn = cnt; break;
                default: break;
            }
        }
        long total = won + pending + rejected + withdrawn;
        double pct = total == 0 ? 0.0 : Math.round(1000.0 * won / total) / 10.0;
        return new com.deharri.jlds.bid.dto.response.BidWinRate(
                won, pending, rejected, withdrawn, total, pct);
    }

    public long countAllAgencyBids(UUID agencyId) {
        return bidRepository.countAllByAgencyId(agencyId);
    }
}
