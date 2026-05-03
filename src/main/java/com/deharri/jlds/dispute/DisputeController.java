package com.deharri.jlds.dispute;

import com.deharri.jlds.dispute.dto.request.SubmitDisputeRequest;
import com.deharri.jlds.dispute.dto.response.DisputeResponse;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Disputes", description = "Job dispute resolution")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/disputes")
    @Operation(summary = "Submit a dispute on a job")
    public ResponseEntity<DisputeResponse> submitDispute(
            @Valid @RequestBody SubmitDisputeRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disputeService.submitDispute(request, userId));
    }

    @GetMapping("/disputes/my")
    @Operation(summary = "List my disputes (paginated)")
    public ResponseEntity<Page<DisputeResponse>> getMyDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(disputeService.getMyDisputes(userId, page, size));
    }

    @GetMapping("/disputes/{disputeId}")
    @Operation(summary = "Get a dispute by ID (only the raiser may view)")
    public ResponseEntity<DisputeResponse> getDispute(
            @PathVariable UUID disputeId,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(disputeService.getDispute(disputeId, userId));
    }

    @GetMapping("/listings/{jobId}/disputes/my")
    @Operation(summary = "List my disputes filed on a specific job")
    public ResponseEntity<List<DisputeResponse>> getMyDisputesForJob(
            @PathVariable UUID jobId,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(disputeService.getMyDisputesForJob(jobId, userId));
    }

    @GetMapping("/agencies/{agencyId}/disputes")
    @Operation(summary = "List all disputes filed on any job assigned to the given agency. Caller must own the agency.")
    public ResponseEntity<List<DisputeResponse>> getDisputesForAgency(
            @PathVariable UUID agencyId,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(disputeService.getDisputesForAgency(agencyId, callerId));
    }

    @PutMapping("/disputes/{disputeId}/status")
    @Operation(summary = "Agency owner: update the status of a dispute (OPEN → UNDER_REVIEW → RESOLVED → CLOSED)")
    public ResponseEntity<DisputeResponse> updateStatus(
            @PathVariable UUID disputeId,
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        com.deharri.jlds.dispute.entity.Dispute.DisputeStatus newStatus =
                com.deharri.jlds.dispute.entity.Dispute.DisputeStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(disputeService.updateStatusForAgency(disputeId, newStatus, callerId));
    }

    @PutMapping("/disputes/{disputeId}/note")
    @Operation(summary = "Agency owner: set an internal note on a dispute")
    public ResponseEntity<DisputeResponse> updateNote(
            @PathVariable UUID disputeId,
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(disputeService.updateNoteForAgency(disputeId, body.get("note"), callerId));
    }
}
