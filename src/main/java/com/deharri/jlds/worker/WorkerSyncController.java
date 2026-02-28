package com.deharri.jlds.worker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/workers")
@RequiredArgsConstructor
@Tag(name = "Worker Sync (Internal)", description = "Internal endpoints for worker profile synchronization")
public class WorkerSyncController {

    private final WorkerProfileService workerProfileService;

    @PostMapping("/sync-all")
    @Operation(summary = "Trigger full worker sync from UMS")
    public ResponseEntity<Map<String, Object>> syncAll() {
        int count = workerProfileService.syncAllWorkers();
        return ResponseEntity.ok(Map.of("syncedCount", count, "status", "completed"));
    }

    @PostMapping("/sync")
    @Operation(summary = "Receive single worker update (webhook)")
    public ResponseEntity<Map<String, String>> syncSingleWorker(@RequestBody Map<String, Object> workerData) {
        workerProfileService.syncSingleWorker(workerData);
        return ResponseEntity.ok(Map.of("status", "synced"));
    }
}
