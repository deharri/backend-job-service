package com.deharri.jlds.review;

import com.deharri.jlds.review.dto.request.CreateReviewRequest;
import com.deharri.jlds.review.dto.response.RatingSummary;
import com.deharri.jlds.review.dto.response.ReviewListResponse;
import com.deharri.jlds.review.dto.response.ReviewResponse;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Bidirectional review management")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/listings/{jobId}/reviews")
    @Operation(summary = "Leave a review for a completed job")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        UUID reviewerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(jobId, request, reviewerId));
    }

    @GetMapping("/listings/{jobId}/reviews")
    @Operation(summary = "Get reviews for a job")
    public ResponseEntity<List<ReviewResponse>> getReviewsForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(reviewService.getReviewsForJob(jobId));
    }

    @GetMapping("/reviews/worker/{workerId}")
    @Operation(summary = "Get worker's reviews (paginated)")
    public ResponseEntity<ReviewListResponse> getWorkerReviews(
            @PathVariable UUID workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getWorkerReviews(workerId, page, size));
    }

    @GetMapping("/reviews/worker/{workerId}/summary")
    @Operation(summary = "Get worker's aggregated rating summary")
    public ResponseEntity<RatingSummary> getWorkerRatingSummary(@PathVariable UUID workerId) {
        return ResponseEntity.ok(reviewService.getWorkerRatingSummary(workerId));
    }

    @GetMapping("/reviews/consumer/{consumerId}")
    @Operation(summary = "Get consumer's reviews (paginated)")
    public ResponseEntity<ReviewListResponse> getConsumerReviews(
            @PathVariable UUID consumerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getConsumerReviews(consumerId, page, size));
    }

    @GetMapping("/reviews/consumer/{consumerId}/summary")
    @Operation(summary = "Get consumer's aggregated rating summary")
    public ResponseEntity<RatingSummary> getConsumerRatingSummary(@PathVariable UUID consumerId) {
        return ResponseEntity.ok(reviewService.getConsumerRatingSummary(consumerId));
    }
}
