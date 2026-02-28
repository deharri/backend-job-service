package com.deharri.jlds.listing;

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
    @Operation(summary = "Mark job as completed (assigned worker only)")
    public ResponseEntity<JobListingResponse> completeJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(jobListingService.completeJob(jobId, workerId));
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
}
