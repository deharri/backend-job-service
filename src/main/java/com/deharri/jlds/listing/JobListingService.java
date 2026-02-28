package com.deharri.jlds.listing;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobListingService {

    private final JobListingRepository jobListingRepository;
    private final JobListingMapper jobListingMapper;

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
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse startJob(UUID jobId, UUID workerId) {
        JobListing listing = findListingOrThrow(jobId);

        if (!workerId.equals(listing.getAssignedWorkerId())) {
            throw new UnauthorizedAccessException("Only the assigned worker can start this job");
        }
        if (listing.getStatus() != JobStatus.ASSIGNED) {
            throw new InvalidOperationException("Job must be in ASSIGNED status to start");
        }

        listing.setStatus(JobStatus.IN_PROGRESS);
        listing.setStartedAt(Instant.now());
        listing = jobListingRepository.save(listing);
        log.info("Job started: {} by worker: {}", jobId, workerId);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse completeJob(UUID jobId, UUID workerId) {
        JobListing listing = findListingOrThrow(jobId);

        if (!workerId.equals(listing.getAssignedWorkerId())) {
            throw new UnauthorizedAccessException("Only the assigned worker can complete this job");
        }
        if (listing.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidOperationException("Job must be IN_PROGRESS to mark as completed");
        }

        listing.setStatus(JobStatus.COMPLETED);
        listing.setCompletedAt(Instant.now());
        listing = jobListingRepository.save(listing);
        log.info("Job completed: {} by worker: {}", jobId, workerId);
        return jobListingMapper.toResponse(listing);
    }

    @Transactional
    public JobListingResponse confirmCompletion(UUID jobId, UUID consumerId) {
        JobListing listing = findListingOrThrow(jobId);
        verifyOwnership(listing, consumerId);

        if (listing.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidOperationException("Job must be in COMPLETED status to confirm completion");
        }

        // Already completed by worker, consumer confirms — status stays COMPLETED
        log.info("Consumer confirmed completion of job: {}", jobId);
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
        Page<JobListing> listings = jobListingRepository.findByAssignedWorkerIdOrderByCreatedAtDesc(workerId, pageable);

        return JobListingListResponse.builder()
                .listings(listings.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(listings.getNumber())
                .size(listings.getSize())
                .totalElements(listings.getTotalElements())
                .totalPages(listings.getTotalPages())
                .build();
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
}
