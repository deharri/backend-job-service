package com.deharri.jlds.notifications;

import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.notifications.dto.response.AgencyNotificationDto;
import com.deharri.jlds.notifications.dto.response.UnreadSummaryDto;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Agency notification feed and SSE event stream")
public class AgencyNotificationController {

    private final AgencyNotificationService service;
    private final SseEmitterRegistry sseRegistry;
    private final JobListingService jobListingService;

    @GetMapping("/agencies/{agencyId}/notifications")
    @Operation(summary = "List agency notifications (paginated, newest first)")
    public ResponseEntity<Page<AgencyNotificationDto>> list(
            @PathVariable UUID agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            HttpServletRequest req) {
        UUID callerId = HeaderExtractor.getUserId(req);
        return ResponseEntity.ok(service.list(agencyId, callerId, page, size));
    }

    @GetMapping("/agencies/{agencyId}/notifications/unread")
    @Operation(summary = "Return the unread notification count for the agency")
    public ResponseEntity<UnreadSummaryDto> unread(
            @PathVariable UUID agencyId,
            HttpServletRequest req) {
        UUID callerId = HeaderExtractor.getUserId(req);
        return ResponseEntity.ok(service.unreadSummary(agencyId, callerId));
    }

    @PutMapping("/agencies/{agencyId}/notifications/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID agencyId,
            @PathVariable UUID notificationId,
            HttpServletRequest req) {
        UUID callerId = HeaderExtractor.getUserId(req);
        service.markRead(agencyId, notificationId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/agencies/{agencyId}/notifications/read-all")
    @Operation(summary = "Mark all unread notifications as read")
    public ResponseEntity<Void> markAllRead(
            @PathVariable UUID agencyId,
            HttpServletRequest req) {
        UUID callerId = HeaderExtractor.getUserId(req);
        service.markAllRead(agencyId, callerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/events/agency/{agencyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to agency real-time event stream (SSE).")
    public SseEmitter stream(
            @PathVariable UUID agencyId,
            HttpServletRequest req) {
        UUID callerId = HeaderExtractor.getUserId(req);
        JobListingService.UmsAgencyInfo info = jobListingService.umsFetchAgencyInfo(agencyId);
        if (info == null || !callerId.equals(info.ownerUserId())) {
            throw new UnauthorizedAccessException("Only the agency owner can subscribe to its event stream");
        }
        return sseRegistry.register(agencyId);
    }
}
