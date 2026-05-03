package com.deharri.jlds.review;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.ReviewType;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.review.dto.request.CreateReviewRequest;
import com.deharri.jlds.review.dto.response.RatingSummary;
import com.deharri.jlds.review.dto.response.ReviewListResponse;
import com.deharri.jlds.review.dto.response.ReviewResponse;
import com.deharri.jlds.review.entity.JobReview;
import com.deharri.jlds.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final JobListingService jobListingService;
    private final JobListingRepository jobListingRepository;
    private final RestTemplate restTemplate;

    private static final String UMS_WORKER_STATS_URL = "http://user-mgmt-service/api/v1/internal/workers/{workerId}/stats";
    private static final String UMS_AGENCY_LOOKUP_URL = "http://user-mgmt-service/api/v1/agencies/internal/{agencyId}";
    private static final String UMS_AGENCY_STATS_URL = "http://user-mgmt-service/api/v1/agencies/internal/{agencyId}/stats";

    @Transactional
    public ReviewResponse createReview(UUID jobId, CreateReviewRequest request, UUID reviewerId) {
        JobListing listing = jobListingService.findListingOrThrow(jobId);

        if (listing.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidOperationException("Can only review completed jobs");
        }

        // Consumer must confirm completion before either party can review
        if (listing.getConsumerConfirmedAt() == null) {
            throw new InvalidOperationException("Consumer must confirm completion before reviews can be left");
        }

        // Determine review type based on reviewer's role in the job
        ReviewType reviewType;
        UUID revieweeId;

        if (listing.getConsumerId().equals(reviewerId)) {
            // Consumer reviewing the performer (worker or agency)
            if (listing.getAssignedAgencyId() != null) {
                reviewType = ReviewType.CONSUMER_TO_AGENCY;
                revieweeId = listing.getAssignedAgencyId();
            } else if (listing.getAssignedWorkerId() != null) {
                reviewType = ReviewType.CONSUMER_TO_WORKER;
                revieweeId = listing.getAssignedWorkerId();
            } else {
                throw new InvalidOperationException("No performer assigned to this job");
            }
        } else if (listing.getAssignedWorkerId() != null && listing.getAssignedWorkerId().equals(reviewerId)) {
            // Worker reviewing the consumer
            reviewType = ReviewType.WORKER_TO_CONSUMER;
            revieweeId = listing.getConsumerId();
        } else if (listing.getAssignedAgencyId() != null) {
            // Possibly: agency owner reviewing the consumer — verify ownership via UMS
            UUID owner = umsFetchAgencyOwnerUserId(listing.getAssignedAgencyId());
            if (owner == null || !owner.equals(reviewerId)) {
                throw new UnauthorizedAccessException("You are not a participant in this job");
            }
            reviewType = ReviewType.AGENCY_TO_CONSUMER;
            revieweeId = listing.getConsumerId();
        } else {
            throw new UnauthorizedAccessException("You are not a participant in this job");
        }

        // Check if review already exists for this direction
        if (reviewRepository.findByJobIdAndReviewType(jobId, reviewType).isPresent()) {
            throw new InvalidOperationException("You have already reviewed this job");
        }

        JobReview review = reviewMapper.toEntity(request);
        review.setJobId(jobId);
        review.setReviewerId(reviewType == ReviewType.AGENCY_TO_CONSUMER
                ? listing.getAssignedAgencyId()
                : reviewerId);
        review.setRevieweeId(revieweeId);
        review.setReviewType(reviewType);

        review = reviewRepository.save(review);
        log.info("Review created: {} for job: {} type: {}", review.getReviewId(), jobId, reviewType);

        // Dual-write: when a customer reviews an agency-fulfilled job that was dispatched to a worker,
        // also write a CONSUMER_TO_WORKER row targeting the dispatched worker so they get the rating too.
        if (reviewType == ReviewType.CONSUMER_TO_AGENCY
                && listing.getDispatchedWorkerId() != null
                && reviewRepository.findByJobIdAndReviewType(jobId, ReviewType.CONSUMER_TO_WORKER).isEmpty()) {
            JobReview workerReview = reviewMapper.toEntity(request);
            workerReview.setJobId(jobId);
            workerReview.setReviewerId(reviewerId);
            workerReview.setRevieweeId(listing.getDispatchedWorkerId());
            workerReview.setReviewType(ReviewType.CONSUMER_TO_WORKER);
            reviewRepository.save(workerReview);
            log.info("Dual-write: also created CONSUMER_TO_WORKER review for dispatched worker {} on job {}",
                    listing.getDispatchedWorkerId(), jobId);
        }

        // Sync stats to UMS after consumer-side reviews
        if (reviewType == ReviewType.CONSUMER_TO_WORKER && listing.getAssignedWorkerId() != null) {
            syncWorkerStatsToUms(listing.getAssignedWorkerId());
        } else if (reviewType == ReviewType.CONSUMER_TO_AGENCY) {
            if (listing.getAssignedAgencyId() != null) {
                syncAgencyStatsToUms(listing.getAssignedAgencyId());
            }
            if (listing.getDispatchedWorkerId() != null) {
                syncWorkerStatsToUms(listing.getDispatchedWorkerId());
            }
        }

        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForJob(UUID jobId) {
        jobListingService.findListingOrThrow(jobId);
        return reviewRepository.findByJobId(jobId).stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getWorkerReviews(UUID workerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobReview> reviews = reviewRepository.findByRevieweeIdAndReviewTypeOrderByCreatedAtDesc(
                workerId, ReviewType.CONSUMER_TO_WORKER, pageable);

        return ReviewListResponse.builder()
                .reviews(reviews.getContent().stream().map(reviewMapper::toResponse).toList())
                .page(reviews.getNumber())
                .size(reviews.getSize())
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public RatingSummary getWorkerRatingSummary(UUID workerId) {
        Double avgRating = reviewRepository.getAverageRating(workerId, ReviewType.CONSUMER_TO_WORKER);
        Long totalReviews = reviewRepository.countReviews(workerId, ReviewType.CONSUMER_TO_WORKER);

        return RatingSummary.builder()
                .userId(workerId)
                .averageRating(toBigDecimal(avgRating))
                .totalReviews(totalReviews)
                .averageQualityRating(toBigDecimal(reviewRepository.getAvgQualityRating(workerId)))
                .averagePunctualityRating(toBigDecimal(reviewRepository.getAvgPunctualityRating(workerId)))
                .averageCommunicationRating(toBigDecimal(reviewRepository.getAvgCommunicationRating(workerId)))
                .averageValueRating(toBigDecimal(reviewRepository.getAvgValueRating(workerId)))
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getConsumerReviews(UUID consumerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobReview> reviews = reviewRepository.findByRevieweeIdAndReviewTypeOrderByCreatedAtDesc(
                consumerId, ReviewType.WORKER_TO_CONSUMER, pageable);

        return ReviewListResponse.builder()
                .reviews(reviews.getContent().stream().map(reviewMapper::toResponse).toList())
                .page(reviews.getNumber())
                .size(reviews.getSize())
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public RatingSummary getConsumerRatingSummary(UUID consumerId) {
        Double avgRating = reviewRepository.getAverageRating(consumerId, ReviewType.WORKER_TO_CONSUMER);
        Long totalReviews = reviewRepository.countReviews(consumerId, ReviewType.WORKER_TO_CONSUMER);

        return RatingSummary.builder()
                .userId(consumerId)
                .averageRating(toBigDecimal(avgRating))
                .totalReviews(totalReviews)
                .averageCommunicationRating(toBigDecimal(reviewRepository.getAvgCommunicationRating(consumerId)))
                .averageReliabilityRating(toBigDecimal(reviewRepository.getAvgReliabilityRating(consumerId)))
                .build();
    }

    private void syncWorkerStatsToUms(UUID workerId) {
        try {
            Double avgRating = reviewRepository.getAverageRating(workerId, ReviewType.CONSUMER_TO_WORKER);
            long completedJobs = jobListingRepository.countConfirmedCompletedByWorkerId(workerId);

            Map<String, Object> statsDto = Map.of(
                    "averageRating", avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    "totalJobsCompleted", (int) completedJobs
            );

            String url = UMS_WORKER_STATS_URL.replace("{workerId}", workerId.toString());
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(statsDto), Void.class);
            log.info("Synced worker stats to UMS for worker {}: rating={}, jobs={}", workerId, avgRating, completedJobs);
        } catch (Exception e) {
            log.error("Failed to sync worker stats to UMS for worker {}: {}", workerId, e.getMessage());
        }
    }

    private UUID umsFetchAgencyOwnerUserId(UUID agencyId) {
        try {
            String url = UMS_AGENCY_LOOKUP_URL.replace("{agencyId}", agencyId.toString());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("ownerUserId") == null) {
                return null;
            }
            return UUID.fromString(body.get("ownerUserId").toString());
        } catch (Exception e) {
            log.error("Failed to fetch agency owner from UMS for agency {}: {}", agencyId, e.getMessage());
            return null;
        }
    }

    private void syncAgencyStatsToUms(UUID agencyId) {
        try {
            Double avgRating = reviewRepository.getAverageRating(agencyId, ReviewType.CONSUMER_TO_AGENCY);
            long completedJobs = jobListingRepository.countConfirmedCompletedByAgencyId(agencyId);

            Map<String, Object> statsDto = Map.of(
                    "averageRating", avgRating != null
                            ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO,
                    "totalJobsCompleted", (int) completedJobs
            );

            String url = UMS_AGENCY_STATS_URL.replace("{agencyId}", agencyId.toString());
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(statsDto), Void.class);
            log.info("Synced agency stats to UMS for agency {}: rating={}, jobs={}", agencyId, avgRating, completedJobs);
        } catch (Exception e) {
            log.error("Failed to sync agency stats to UMS for agency {}: {}", agencyId, e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
