package com.deharri.jlds.saved;

import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Saved Jobs", description = "Save and manage bookmarked job listings")
public class SavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping("/listings/{jobId}/save")
    @Operation(summary = "Save a job listing")
    public ResponseEntity<Void> saveJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        savedJobService.saveJob(userId, jobId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/listings/{jobId}/save")
    @Operation(summary = "Unsave a job listing")
    public ResponseEntity<Void> unsaveJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        savedJobService.unsaveJob(userId, jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/saved-jobs")
    @Operation(summary = "Get saved job listings")
    public ResponseEntity<JobListingListResponse> getSavedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(savedJobService.getSavedJobs(userId, page, size));
    }
}
