package com.deharri.jlds.bid;

import com.deharri.jlds.bid.dto.request.CreateBidRequest;
import com.deharri.jlds.bid.dto.request.UpdateBidRequest;
import com.deharri.jlds.bid.dto.response.BidListResponse;
import com.deharri.jlds.bid.dto.response.BidResponse;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Job bidding management")
public class BidController {

    private final BidService bidService;
    private final JobListingService jobListingService;

    @PostMapping("/listings/{jobId}/bids")
    @Operation(summary = "Place a bid on a job listing")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateBidRequest request,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        String username = HeaderExtractor.getUsername(httpRequest);

        // Agency ownership check: if this is an agency bid, the caller must own that agency.
        if (request.getAgencyId() != null) {
            UUID agencyOwner = jobListingService.umsFetchAgencyOwnerUserId(request.getAgencyId());
            if (agencyOwner == null || !callerId.equals(agencyOwner)) {
                throw new UnauthorizedAccessException("You are not the owner of agency " + request.getAgencyId());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.placeBid(jobId, request, callerId, username));
    }

    @GetMapping("/listings/{jobId}/bids")
    @Operation(summary = "List bids for a job (consumer/owner only)")
    public ResponseEntity<BidListResponse> getBidsForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.getBidsForJob(jobId, consumerId, page, size));
    }

    @GetMapping("/bids/{bidId}")
    @Operation(summary = "Get bid details")
    public ResponseEntity<BidResponse> getBidById(
            @PathVariable UUID bidId,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.getBidById(bidId, userId));
    }

    @PutMapping("/bids/{bidId}")
    @Operation(summary = "Update a bid")
    public ResponseEntity<BidResponse> updateBid(
            @PathVariable UUID bidId,
            @Valid @RequestBody UpdateBidRequest request,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.updateBid(bidId, request, workerId));
    }

    @PutMapping("/bids/{bidId}/withdraw")
    @Operation(summary = "Withdraw a bid")
    public ResponseEntity<BidResponse> withdrawBid(
            @PathVariable UUID bidId,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.withdrawBid(bidId, workerId));
    }

    @PutMapping("/bids/{bidId}/accept")
    @Operation(summary = "Accept a bid (consumer/owner only)")
    public ResponseEntity<BidResponse> acceptBid(
            @PathVariable UUID bidId,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.acceptBid(bidId, consumerId));
    }

    @PutMapping("/bids/{bidId}/reject")
    @Operation(summary = "Reject a bid (consumer/owner only)")
    public ResponseEntity<BidResponse> rejectBid(
            @PathVariable UUID bidId,
            HttpServletRequest httpRequest) {
        UUID consumerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.rejectBid(bidId, consumerId));
    }

    @GetMapping("/bids/my-bids")
    @Operation(summary = "Get worker's own bids")
    public ResponseEntity<BidListResponse> getMyBids(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID workerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.getMyBids(workerId, page, size));
    }

    @GetMapping("/bids/my-agency-bids")
    @Operation(summary = "Get bids placed by the caller's agency (caller must be the agency owner)")
    public ResponseEntity<BidListResponse> getMyAgencyBids(
            @RequestParam UUID agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.getMyAgencyBids(agencyId, callerId, page, size));
    }

    @GetMapping("/bids/agency/{agencyId}/analytics/win-rate")
    @Operation(summary = "Win-rate breakdown for the agency's bids over the given range.")
    public ResponseEntity<com.deharri.jlds.bid.dto.response.BidWinRate> agencyWinRate(
            @PathVariable UUID agencyId,
            @RequestParam java.time.Instant from,
            @RequestParam java.time.Instant to,
            HttpServletRequest httpRequest) {
        UUID callerId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(bidService.getAgencyWinRate(agencyId, callerId, from, to));
    }
}
