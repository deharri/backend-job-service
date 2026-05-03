package com.deharri.jlds.listing;

import com.deharri.jlds.listing.dto.request.CreateJobFromOfferRequest;
import com.deharri.jlds.listing.dto.request.CreateJobListingRequest;
import com.deharri.jlds.listing.dto.request.JobSearchFilter;
import com.deharri.jlds.listing.dto.request.UpdateJobListingRequest;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.dto.response.JobListingResponse;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Job Listings", description = "Job listing CRUD and lifecycle management")
public class JobListingController {

    private final JobListingService jobListingService;
    private final com.deharri.jlds.bid.BidService bidService;

    @PostMapping
    @Operation(summary = "Create a new job listing")
    public ResponseEntity<JobListingResponse> createListing(
            @Valid @RequestBody CreateJobListingRequest request,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        String username = HeaderExtractor.getUsername(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobListingService.createListing(request, consumerId, username));
    }

    @PostMapping("/from-offer")
    @Operation(summary = "Create a job listing from an accepted chat offer")
    public ResponseEntity<JobListingResponse> createJobFromOffer(
            @Valid @RequestBody CreateJobFromOfferRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobListingService.createJobFromOffer(request));
    }

    @GetMapping
    @Operation(summary = "Search and discover job listings")
    public ResponseEntity<JobListingListResponse> searchListings(
            @ModelAttribute JobSearchFilter filter) {
        return ResponseEntity.ok(jobListingService.searchListings(filter));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job listing details")
    public ResponseEntity<JobListingResponse> getListingById(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobListingService.getListingById(jobId));
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update a job listing")
    public ResponseEntity<JobListingResponse> updateListing(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobListingRequest request,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.updateListing(jobId, request, consumerId));
    }

    @PutMapping("/{jobId}/publish")
    @Operation(summary = "Publish a draft listing")
    public ResponseEntity<JobListingResponse> publishListing(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.publishListing(jobId, consumerId));
    }

    @PutMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a job listing")
    public ResponseEntity<JobListingResponse> cancelListing(
            @PathVariable UUID jobId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(jobListingService.cancelListing(jobId, consumerId, reason));
    }

    @PutMapping("/{jobId}/start")
    @Operation(summary = "Mark job as in progress (assigned worker only)")
    public ResponseEntity<JobListingResponse> startJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.startJob(jobId, workerId));
    }

    @PutMapping("/{jobId}/complete")
    @Operation(summary = "Mark job as completed (assigned worker, agency owner, or dispatched worker)")
    public ResponseEntity<JobListingResponse> completeJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.completeJob(jobId, workerId));
    }

    @PutMapping("/{jobId}/dispatch")
    @Operation(summary = "Dispatch an agency-assigned job to a specific worker (agency owner only)")
    public ResponseEntity<JobListingResponse> dispatchJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody com.deharri.jlds.listing.dto.request.DispatchJobRequest request,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.dispatchToWorker(jobId, request.getWorkerId(), callerId));
    }

    @PutMapping("/{jobId}/confirm-completion")
    @Operation(summary = "Confirm job completion (consumer only)")
    public ResponseEntity<JobListingResponse> confirmCompletion(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.confirmCompletion(jobId, consumerId));
    }

    @GetMapping("/my-listings")
    @Operation(summary = "Get consumer's own job listings")
    public ResponseEntity<JobListingListResponse> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.getMyListings(consumerId, page, size));
    }

    @GetMapping("/my-jobs")
    @Operation(summary = "Get worker's assigned jobs")
    public ResponseEntity<JobListingListResponse> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.getMyJobs(workerId, page, size));
    }

    @GetMapping("/agency/{agencyId}")
    @Operation(summary = "Get jobs assigned to an agency (agency-mode dashboard)")
    public ResponseEntity<JobListingListResponse> getAgencyJobs(
            @PathVariable UUID agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(jobListingService.getMyAgencyJobs(agencyId, page, size));
    }

    @GetMapping("/agency/{agencyId}/worker-stats")
    @Operation(summary = "Per-worker stats under an agency: completed-confirmed job counts. " +
            "Powers the future agency-owner analytics dashboard.")
    public ResponseEntity<java.util.Map<String, Long>> getAgencyWorkerStats(
            @PathVariable UUID agencyId,
            @RequestParam UUID workerUserId) {
        long completed = jobListingService.countAgencyJobsForWorker(agencyId, workerUserId);
        return ResponseEntity.ok(java.util.Map.of("completedJobs", completed));
    }

    @GetMapping("/my-dispatched-jobs")
    @Operation(summary = "Get jobs dispatched to the calling worker by their agency")
    public ResponseEntity<JobListingListResponse> getMyDispatchedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.getMyDispatchedJobs(workerId, page, size));
    }

    // ── Analytics ────────────────────────────────────────────────────

    @GetMapping("/agency/{agencyId}/analytics/job-status-counts")
    @Operation(summary = "Counts of agency jobs grouped by status.")
    public ResponseEntity<java.util.List<com.deharri.jlds.listing.dto.response.analytics.StatusCount>>
        agencyStatusCounts(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(jobListingService.agencyStatusCounts(agencyId));
    }

    @GetMapping("/agency/{agencyId}/analytics/jobs-over-time")
    @Operation(summary = "Time series of jobs created per bucket. granularity = day | week | month.")
    public ResponseEntity<java.util.List<com.deharri.jlds.listing.dto.response.analytics.TimeBucket>>
        agencyJobsOverTime(
            @PathVariable UUID agencyId,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam java.time.Instant from,
            @RequestParam java.time.Instant to) {
        return ResponseEntity.ok(jobListingService.agencyJobsOverTime(agencyId, granularity, from, to));
    }

    @GetMapping("/agency/{agencyId}/analytics/jobs-by-city")
    public ResponseEntity<java.util.List<com.deharri.jlds.listing.dto.response.analytics.CityCount>>
        agencyJobsByCity(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(jobListingService.agencyJobsByCity(agencyId));
    }

    @GetMapping("/agency/{agencyId}/analytics/jobs-by-worker-type")
    public ResponseEntity<java.util.List<com.deharri.jlds.listing.dto.response.analytics.WorkerTypeCount>>
        agencyJobsByWorkerType(@PathVariable UUID agencyId) {
        return ResponseEntity.ok(jobListingService.agencyJobsByWorkerType(agencyId));
    }

    @GetMapping("/agency/{agencyId}/analytics/jobs-per-worker")
    public ResponseEntity<java.util.List<com.deharri.jlds.listing.dto.response.analytics.WorkerJobCount>>
        agencyJobsPerWorker(
            @PathVariable UUID agencyId,
            @RequestParam java.time.Instant from,
            @RequestParam java.time.Instant to) {
        return ResponseEntity.ok(jobListingService.agencyJobsPerWorker(agencyId, from, to));
    }

    @GetMapping("/agency/{agencyId}/analytics/funnel")
    @Operation(summary = "Pipeline funnel: bids placed → accepted → dispatched → in progress → completed.")
    public ResponseEntity<com.deharri.jlds.listing.dto.response.analytics.JobsFunnel> agencyFunnel(
            @PathVariable UUID agencyId) {
        long bidsPlaced = bidService.countAllAgencyBids(agencyId);
        return ResponseEntity.ok(jobListingService.agencyJobsFunnel(agencyId, bidsPlaced));
    }
}
