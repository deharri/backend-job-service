package com.deharri.jlds.listing;

import com.deharri.jlds.bid.BidRepository;
import com.deharri.jlds.bid.entity.JobBid;
import com.deharri.jlds.enums.BidStatus;
import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.UrgencyLevel;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.dto.request.CreateJobFromOfferRequest;
import com.deharri.jlds.listing.dto.request.CreateJobListingRequest;
import com.deharri.jlds.listing.dto.request.JobSearchFilter;
import com.deharri.jlds.listing.dto.request.UpdateJobListingRequest;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.dto.response.JobListingResponse;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.listing.mapper.JobListingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobListingService {

    private final JobListingRepository jobListingRepository;
    private final BidRepository bidRepository;
    private final JobListingMapper jobListingMapper;
    private final RestTemplate restTemplate;
    private final com.deharri.jlds.events.JobEventPublisher jobEventPublisher;
    @org.springframework.context.annotation.Lazy
    private final com.deharri.jlds.notifications.AgencyNotificationService agencyNotificationService;

    private static final String UMS_AGENCY_LOOKUP_URL = "http://user-mgmt-service/api/v1/agencies/internal/{agencyId}";
    private static final String UMS_AGENCY_WORKER_MEMBERSHIP_URL = "http://user-mgmt-service/api/v1/agencies/internal/{agencyId}/workers/{workerId}/membership";

    /** Subset of UMS InternalAgencyDto needed for cross-service validation. */
    public record UmsAgencyInfo(UUID agencyId, UUID ownerUserId, boolean subscriptionActive) {}

    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "createdAt", "budgetMax", "urgency", "bidCount"
    );

    @Transactional
    public JobListingResponse createListing(CreateJobListingRequest request, UUID consumerId, String username) {
        JobListing listing = jobListingMapper.toEntity(request);
        listing.setConsumerId(consumerId);
        listing.setConsumerUsername(username);

        if (request.isPublishImmediately()) {
            listing.setStatus(JobStatus.OPEN);
        } else {
            listing.setStatus(JobStatus.DRAFT);
        }

        listing = jobListingRepository.save(listing);
        log.info("Created job listing: {} by user: {}", listing.getJobId(), username);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional(readOnly = true)
    public JobListingListResponse searchListings(JobSearchFilter filter) {
        int size = Math.min(Math.max(filter.getSize(), 1), 50);
        String sortField = VALID_SORT_FIELDS.contains(filter.getSortBy()) ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(filter.getPage(), size, Sort.by(direction, sortField));

        Specification<JobListing> spec = JobListingSpecification.withFilters(
                filter.getWorkerType(),
                filter.getCity(),
                filter.getArea(),
                filter.getBudgetMin(),
                filter.getBudgetMax(),
                filter.getUrgency(),
                filter.getStatus(),
                filter.getSearch()
        );

        Page<JobListing> page = jobListingRepository.findAll(spec, pageable);

        return JobListingListResponse.builder()
                .listings(page.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public JobListingResponse getListingById(UUID jobId) {
        JobListing listing = findListingOrThrow(jobId);
        listing.setViewCount(listing.getViewCount() + 1);
        jobListingRepository.save(listing);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse updateListing(UUID jobId, UpdateJobListingRequest request, UUID consumerId) {
        JobListing listing = findListingOrThrow(jobId);
        verifyOwnership(listing, consumerId);

        if (listing.getStatus() != JobStatus.DRAFT && listing.getStatus() != JobStatus.OPEN) {
            throw new InvalidOperationException("Can only update listings in DRAFT or OPEN status");
        }

        if (request.getTitle() != null) listing.setTitle(request.getTitle());
        if (request.getDescription() != null) listing.setDescription(request.getDescription());
        if (request.getWorkerType() != null) listing.setWorkerType(request.getWorkerType());
        if (request.getTags() != null) listing.setTags(request.getTags());
        if (request.getBudgetMin() != null) listing.setBudgetMin(request.getBudgetMin());
        if (request.getBudgetMax() != null) listing.setBudgetMax(request.getBudgetMax());
        if (request.getBudgetType() != null) listing.setBudgetType(request.getBudgetType());
        if (request.getCity() != null) listing.setCity(request.getCity());
        if (request.getArea() != null) listing.setArea(request.getArea());
        if (request.getLatitude() != null) listing.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) listing.setLongitude(request.getLongitude());
        if (request.getUrgency() != null) listing.setUrgency(request.getUrgency());
        if (request.getPreferredStartDate() != null) listing.setPreferredStartDate(request.getPreferredStartDate());
        if (request.getPreferredEndDate() != null) listing.setPreferredEndDate(request.getPreferredEndDate());
        if (request.getImagePaths() != null) listing.setImagePaths(request.getImagePaths());

        listing = jobListingRepository.save(listing);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse publishListing(UUID jobId, UUID consumerId) {
        JobListing listing = findListingOrThrow(jobId);
        verifyOwnership(listing, consumerId);

        if (listing.getStatus() != JobStatus.DRAFT) {
            throw new InvalidOperationException("Can only publish listings in DRAFT status");
        }

        listing.setStatus(JobStatus.OPEN);
        listing.setExpiresAt(Instant.now().plusSeconds(30L * 24 * 60 * 60));
        listing = jobListingRepository.save(listing);
        log.info("Published job listing: {}", jobId);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse cancelListing(UUID jobId, UUID consumerId, String reason) {
        JobListing listing = findListingOrThrow(jobId);
        verifyOwnership(listing, consumerId);

        if (listing.getStatus() == JobStatus.COMPLETED || listing.getStatus() == JobStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot cancel a listing that is already " + listing.getStatus());
        }

        listing.setStatus(JobStatus.CANCELLED);
        listing.setCancelledAt(Instant.now());
        listing.setCancellationReason(reason);
        listing = jobListingRepository.save(listing);
        log.info("Cancelled job listing: {}", jobId);

        // Publish event — payment-service consumes it and refunds any HELD escrow.
        // (Pre-dispatch cancellations have no payment, which is a no-op on the consumer side.)
        jobEventPublisher.publishCancelled(listing);

        if (listing.getAssignedAgencyId() != null) {
            agencyNotificationService.publish(
                    listing.getAssignedAgencyId(),
                    com.deharri.jlds.notifications.entity.AgencyNotification.Kind.JOB_CANCELLED,
                    listing.getJobId(),
                    "Job cancelled: " + listing.getTitle(),
                    reason == null ? null : "Reason: " + reason);
        }

        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse startJob(UUID jobId, UUID workerId) {
        JobListing listing = findListingOrThrow(jobId);

        boolean isDispatchedWorker = listing.getDispatchedWorkerId() != null
                && workerId.equals(listing.getDispatchedWorkerId());

        if (!isDispatchedWorker) {
            if (listing.getAssignedAgencyId() != null) {
                UUID owner = umsFetchAgencyOwnerUserId(listing.getAssignedAgencyId());
                if (owner == null || !workerId.equals(owner)) {
                    throw new UnauthorizedAccessException("Only the agency owner or dispatched worker can start this job");
                }
            } else if (!workerId.equals(listing.getAssignedWorkerId())) {
                throw new UnauthorizedAccessException("Only the assigned worker can start this job");
            }
        }
        if (listing.getStatus() != JobStatus.ASSIGNED) {
            throw new InvalidOperationException("Job must be in ASSIGNED status to start");
        }

        listing.setStatus(JobStatus.IN_PROGRESS);
        listing.setStartedAt(Instant.now());
        listing = jobListingRepository.save(listing);
        log.info("Job started: {} by caller: {}", jobId, workerId);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse completeJob(UUID jobId, UUID workerId) {
        JobListing listing = findListingOrThrow(jobId);

        boolean isDispatchedWorker = listing.getDispatchedWorkerId() != null
                && workerId.equals(listing.getDispatchedWorkerId());

        if (!isDispatchedWorker) {
            if (listing.getAssignedAgencyId() != null) {
                UUID owner = umsFetchAgencyOwnerUserId(listing.getAssignedAgencyId());
                if (owner == null || !workerId.equals(owner)) {
                    throw new UnauthorizedAccessException("Only the agency owner or dispatched worker can complete this job");
                }
            } else if (!workerId.equals(listing.getAssignedWorkerId())) {
                throw new UnauthorizedAccessException("Only the assigned worker can complete this job");
            }
        }
        if (listing.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidOperationException("Job must be IN_PROGRESS to mark as completed");
        }

        listing.setStatus(JobStatus.COMPLETED);
        listing.setCompletedAt(Instant.now());
        listing = jobListingRepository.save(listing);
        log.info("Job completed: {} by caller: {}", jobId, workerId);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse dispatchToWorker(UUID jobId, UUID workerId, UUID callerId) {
        JobListing listing = findListingOrThrow(jobId);

        if (listing.getAssignedAgencyId() == null) {
            throw new InvalidOperationException("Job is not assigned to an agency");
        }

        UUID owner = umsFetchAgencyOwnerUserId(listing.getAssignedAgencyId());
        if (owner == null || !callerId.equals(owner)) {
            throw new UnauthorizedAccessException("Only the agency owner can dispatch this job");
        }

        // Only first-time dispatch, only on ASSIGNED status. Once a worker is dispatched and
        // work has started, the agency can't silently swap workers — they must cancel + restart.
        if (listing.getStatus() != JobStatus.ASSIGNED) {
            throw new InvalidOperationException("Dispatch is only allowed while job status is ASSIGNED");
        }
        if (listing.getDispatchedWorkerId() != null) {
            throw new InvalidOperationException(
                    "A worker is already dispatched on this job. Cancel and re-list to change.");
        }

        // Verify the worker actually belongs to this agency.
        if (!umsCheckWorkerInAgency(listing.getAssignedAgencyId(), workerId)) {
            throw new InvalidOperationException(
                    "Cannot dispatch worker " + workerId + " — they are not a member of this agency");
        }

        // Best-effort username lookup via UMS (soft-fails if UMS is unavailable)
        String workerUsername = null;
        try {
            String url = "http://user-mgmt-service/api/v1/internal/users/" + workerId;
            org.springframework.http.ResponseEntity<java.util.Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<java.util.Map<String, Object>>() {});
            java.util.Map<String, Object> body = response.getBody();
            if (body != null && body.get("username") != null) {
                workerUsername = body.get("username").toString();
            }
        } catch (Exception e) {
            log.warn("Could not resolve worker {} username from UMS: {}", workerId, e.getMessage());
        }

        listing.setDispatchedWorkerId(workerId);
        listing.setDispatchedWorkerUsername(workerUsername);
        listing = jobListingRepository.save(listing);
        log.info("Job {} dispatched to worker {} by agency owner {}", jobId, workerId, callerId);

        if (listing.getAssignedAgencyId() != null) {
            agencyNotificationService.publish(
                    listing.getAssignedAgencyId(),
                    com.deharri.jlds.notifications.entity.AgencyNotification.Kind.JOB_DISPATCHED,
                    listing.getJobId(),
                    "Worker dispatched on " + listing.getTitle(),
                    "Worker @" + listing.getDispatchedWorkerUsername());
        }

        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse confirmCompletion(UUID jobId, UUID consumerId) {
        JobListing listing = findListingOrThrow(jobId);
        verifyOwnership(listing, consumerId);

        if (listing.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidOperationException("Job must be in COMPLETED status to confirm completion");
        }

        listing.setConsumerConfirmedAt(Instant.now());
        listing = jobListingRepository.save(listing);
        log.info("Consumer confirmed completion of job: {}", jobId);

        // Publish event — UMS consumes for stats sync (worker + agency totalJobsCompleted),
        // payment-service consumes for escrow auto-release. Both are async and decoupled.
        jobEventPublisher.publishConfirmed(listing);

        if (listing.getAssignedAgencyId() != null) {
            agencyNotificationService.publish(
                    listing.getAssignedAgencyId(),
                    com.deharri.jlds.notifications.entity.AgencyNotification.Kind.JOB_CONFIRMED,
                    listing.getJobId(),
                    "Job completion confirmed by consumer",
                    listing.getTitle());
        }

        return jobListingMapper.toResponse(listing);
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getMyListings(UUID consumerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobListing> listings = jobListingRepository.findByConsumerIdOrderByCreatedAtDesc(consumerId, pageable);

        return JobListingListResponse.builder()
                .listings(listings.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(listings.getNumber())
                .size(listings.getSize())
                .totalElements(listings.getTotalElements())
                .totalPages(listings.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getMyJobs(UUID workerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        // Includes both directly-assigned (own bid accepted) AND agency-dispatched jobs,
        // so workers in an agency see their dispatched assignments here.
        Page<JobListing> listings = jobListingRepository.findMyJobsAsWorker(workerId, pageable);

        return JobListingListResponse.builder()
                .listings(listings.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(listings.getNumber())
                .size(listings.getSize())
                .totalElements(listings.getTotalElements())
                .totalPages(listings.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getMyAgencyJobs(UUID agencyId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobListing> listings = jobListingRepository.findByAssignedAgencyIdOrderByCreatedAtDesc(agencyId, pageable);
        return JobListingListResponse.builder()
                .listings(listings.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(listings.getNumber())
                .size(listings.getSize())
                .totalElements(listings.getTotalElements())
                .totalPages(listings.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getMyDispatchedJobs(UUID workerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobListing> listings = jobListingRepository.findByDispatchedWorkerIdOrderByCreatedAtDesc(workerId, pageable);
        return JobListingListResponse.builder()
                .listings(listings.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(listings.getNumber())
                .size(listings.getSize())
                .totalElements(listings.getTotalElements())
                .totalPages(listings.getTotalPages())
                .build();
    }

    @Transactional
    public JobListingResponse createJobFromOffer(CreateJobFromOfferRequest request) {
        boolean hasWorker = request.getWorkerId() != null;
        boolean hasAgency = request.getAgencyId() != null;
        // XOR: exactly one of workerId/agencyId must be set (both-true and both-false both throw)
        if (hasWorker == hasAgency) {
            throw new InvalidOperationException("Exactly one of workerId or agencyId must be provided");
        }

        // Build listing with ASSIGNED status
        JobListing.JobListingBuilder b = JobListing.builder()
                .consumerId(request.getConsumerId())
                .consumerUsername(request.getConsumerUsername())
                .consumerFirstName(request.getConsumerFirstName())
                .consumerLastName(request.getConsumerLastName())
                .title(request.getTitle())
                .description(request.getDescription())
                .workerType(request.getWorkerType())
                .budgetMin(request.getBudgetAmount())
                .budgetMax(request.getBudgetAmount())
                .budgetType(request.getBudgetType() != null ? request.getBudgetType() : BudgetType.FIXED)
                .city(request.getCity())
                .area(request.getArea())
                .urgency(UrgencyLevel.NORMAL)
                .status(JobStatus.ASSIGNED)
                .assignedAt(Instant.now());

        if (hasWorker) {
            b.assignedWorkerId(request.getWorkerId())
             .assignedWorkerUsername(request.getWorkerUsername());
        } else {
            b.assignedAgencyId(request.getAgencyId())
             .assignedAgencyName(request.getAgencyName());
        }

        JobListing listing = b.build();
        listing = jobListingRepository.save(listing);

        if (hasWorker) {
            // Build accepted bid
            JobBid bid = JobBid.builder()
                    .jobId(listing.getJobId())
                    .workerId(request.getWorkerId())
                    .workerUsername(request.getWorkerUsername())
                    .workerFirstName(request.getWorkerFirstName())
                    .workerLastName(request.getWorkerLastName())
                    .workerWorkerType(request.getWorkerWorkerType())
                    .workerExperienceYears(request.getWorkerExperienceYears())
                    .workerRating(request.getWorkerRating())
                    .proposedAmount(request.getBudgetAmount())
                    .proposedRateType(request.getBudgetType() != null ? request.getBudgetType() : BudgetType.FIXED)
                    .coverMessage("Accepted from chat offer")
                    .estimatedDays(request.getDeliveryDays())
                    .status(BidStatus.ACCEPTED)
                    .build();

            bid = bidRepository.save(bid);

            // Link bid to listing
            listing.setAssignedBidId(bid.getBidId());
            listing.setBidCount(1);
            listing = jobListingRepository.save(listing);
        }

        log.info("Created job from offer: {} assigned to {}: {}",
                listing.getJobId(),
                hasWorker ? "worker" : "agency",
                hasWorker ? request.getWorkerId() : request.getAgencyId());
        return jobListingMapper.toResponse(listing);
    }

    // --- Internal helpers ---

    public JobListing findListingOrThrow(UUID jobId) {
        return jobListingRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job listing not found: " + jobId));
    }

    private void verifyOwnership(JobListing listing, UUID consumerId) {
        if (!listing.getConsumerId().equals(consumerId)) {
            throw new UnauthorizedAccessException("You do not own this job listing");
        }
    }

    /**
     * Count of completed-and-confirmed jobs the worker did under the given agency.
     * Used by the agency-owner analytics endpoint.
     */
    @Transactional(readOnly = true)
    public long countAgencyJobsForWorker(UUID agencyId, UUID workerUserId) {
        return jobListingRepository.countCompletedByAgencyAndWorker(agencyId, workerUserId);
    }

    public UUID umsFetchAgencyOwnerUserId(UUID agencyId) {
        UmsAgencyInfo info = umsFetchAgencyInfo(agencyId);
        return info == null ? null : info.ownerUserId();
    }

    /** Fetches the full agency info DTO from UMS. Returns null on failure. */
    public UmsAgencyInfo umsFetchAgencyInfo(UUID agencyId) {
        try {
            String url = UMS_AGENCY_LOOKUP_URL.replace("{agencyId}", agencyId.toString());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body == null) return null;

            UUID ownerUserId = body.get("ownerUserId") == null ? null
                    : UUID.fromString(body.get("ownerUserId").toString());
            boolean active = body.get("subscriptionActive") instanceof Boolean b ? b : false;
            return new UmsAgencyInfo(agencyId, ownerUserId, active);
        } catch (Exception e) {
            log.error("Failed to fetch agency info from UMS for agency {}: {}", agencyId, e.getMessage());
            return null;
        }
    }

    /** Asks UMS whether the worker (by user-id) belongs to the given agency. Defaults to false on failure. */
    public boolean umsCheckWorkerInAgency(UUID agencyId, UUID workerUserId) {
        try {
            String url = UMS_AGENCY_WORKER_MEMBERSHIP_URL
                    .replace("{agencyId}", agencyId.toString())
                    .replace("{workerId}", workerUserId.toString());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body == null) return false;
            Object member = body.get("member");
            return member instanceof Boolean b && b;
        } catch (Exception e) {
            log.error("Failed to verify worker {} in agency {} via UMS: {}", workerUserId, agencyId, e.getMessage());
            return false;
        }
    }

    // ── Analytics ────────────────────────────────────────────────────

    public List<com.deharri.jlds.listing.dto.response.analytics.StatusCount> agencyStatusCounts(UUID agencyId) {
        return jobListingRepository.aggStatusCounts(agencyId).stream()
                .map(r -> new com.deharri.jlds.listing.dto.response.analytics.StatusCount(
                        (String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<com.deharri.jlds.listing.dto.response.analytics.TimeBucket> agencyJobsOverTime(
            UUID agencyId, String granularity, java.time.Instant from, java.time.Instant to) {
        validateGranularity(granularity);
        return jobListingRepository.aggJobsOverTime(agencyId, granularity, from, to).stream()
                .map(r -> {
                    java.time.Instant b = r[0] instanceof java.sql.Timestamp ts
                            ? ts.toInstant()
                            : ((java.time.OffsetDateTime) r[0]).toInstant();
                    return new com.deharri.jlds.listing.dto.response.analytics.TimeBucket(
                            b, ((Number) r[1]).longValue());
                })
                .toList();
    }

    public List<com.deharri.jlds.listing.dto.response.analytics.CityCount> agencyJobsByCity(UUID agencyId) {
        return jobListingRepository.aggJobsByCity(agencyId).stream()
                .map(r -> new com.deharri.jlds.listing.dto.response.analytics.CityCount(
                        (String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<com.deharri.jlds.listing.dto.response.analytics.WorkerTypeCount> agencyJobsByWorkerType(UUID agencyId) {
        return jobListingRepository.aggJobsByWorkerType(agencyId).stream()
                .map(r -> new com.deharri.jlds.listing.dto.response.analytics.WorkerTypeCount(
                        (String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<com.deharri.jlds.listing.dto.response.analytics.WorkerJobCount> agencyJobsPerWorker(
            UUID agencyId, java.time.Instant from, java.time.Instant to) {
        return jobListingRepository.aggJobsPerWorker(agencyId, from, to).stream()
                .map(r -> new com.deharri.jlds.listing.dto.response.analytics.WorkerJobCount(
                        (UUID) r[0], (String) r[1], ((Number) r[2]).longValue()))
                .toList();
    }

    public com.deharri.jlds.listing.dto.response.analytics.JobsFunnel agencyJobsFunnel(UUID agencyId, long bidsPlaced) {
        Object[] r = (Object[]) jobListingRepository.aggJobsFunnel(agencyId);
        long accepted = ((Number) r[0]).longValue();
        long dispatched = ((Number) r[1]).longValue();
        long inProgress = ((Number) r[2]).longValue();
        long completed = ((Number) r[3]).longValue();
        return new com.deharri.jlds.listing.dto.response.analytics.JobsFunnel(
                bidsPlaced, accepted, dispatched, inProgress, completed);
    }

    private void validateGranularity(String g) {
        if (!"day".equals(g) && !"week".equals(g) && !"month".equals(g)) {
            throw new com.deharri.jlds.error.exception.InvalidOperationException(
                    "granularity must be 'day', 'week', or 'month'");
        }
    }

}
