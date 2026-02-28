package com.deharri.jlds.worker;

import com.deharri.jlds.enums.WorkerType;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.worker.dto.request.WorkerSearchFilter;
import com.deharri.jlds.worker.dto.response.WorkerProfileListResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workers")
@RequiredArgsConstructor
@Tag(name = "Worker Discovery", description = "Search and discover workers")
public class WorkerDiscoveryController {

    private final WorkerProfileService workerProfileService;

    @GetMapping
    @Operation(summary = "Search and discover workers")
    public ResponseEntity<WorkerProfileListResponse> searchWorkers(
            @ModelAttribute WorkerSearchFilter filter) {
        return ResponseEntity.ok(workerProfileService.searchWorkers(filter));
    }

    @GetMapping("/{workerId}")
    @Operation(summary = "Get worker profile")
    public ResponseEntity<WorkerProfileResponse> getWorkerProfile(@PathVariable UUID workerId) {
        return ResponseEntity.ok(workerProfileService.getWorkerProfile(workerId));
    }

    @GetMapping("/{workerId}/jobs")
    @Operation(summary = "Get worker's completed jobs")
    public ResponseEntity<JobListingListResponse> getWorkerJobs(
            @PathVariable UUID workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workerProfileService.getWorkerCompletedJobs(workerId, page, size));
    }

    @GetMapping("/types")
    @Operation(summary = "Get all worker types")
    public ResponseEntity<List<Map<String, String>>> getWorkerTypes() {
        List<Map<String, String>> types = Arrays.stream(WorkerType.values())
                .map(t -> Map.of(
                        "name", t.name(),
                        "displayName", t.getDisplayName(),
                        "description", t.getDescription()
                ))
                .toList();
        return ResponseEntity.ok(types);
    }
}
