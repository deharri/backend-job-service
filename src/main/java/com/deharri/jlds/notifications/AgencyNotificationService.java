package com.deharri.jlds.notifications;

import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.notifications.dto.response.AgencyNotificationDto;
import com.deharri.jlds.notifications.dto.response.UnreadSummaryDto;
import com.deharri.jlds.notifications.entity.AgencyNotification;
import com.deharri.jlds.notifications.entity.AgencyNotification.Kind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class AgencyNotificationService {

    private final AgencyNotificationRepository repo;
    private final SseEmitterRegistry sseRegistry;
    // JobListingService also depends on this bean (it calls publish(...) on dispatch/confirm),
    // so we resolve lazily to break the constructor-time cycle.
    private final JobListingService jobListingService;

    public AgencyNotificationService(
            AgencyNotificationRepository repo,
            SseEmitterRegistry sseRegistry,
            @Lazy JobListingService jobListingService) {
        this.repo = repo;
        this.sseRegistry = sseRegistry;
        this.jobListingService = jobListingService;
    }

    @Transactional
    public void publish(UUID agencyId, Kind kind, UUID jobId, String title, String body) {
        if (agencyId == null) return;
        AgencyNotification n = AgencyNotification.builder()
                .agencyId(agencyId)
                .kind(kind)
                .jobId(jobId)
                .title(title)
                .body(body)
                .build();
        n = repo.save(n);
        log.info("Notification {} ({}) for agency {} job {}", n.getNotificationId(), kind, agencyId, jobId);
        sseRegistry.broadcast(agencyId, kind.name(), toDto(n));
    }

    @Transactional(readOnly = true)
    public Page<AgencyNotificationDto> list(UUID agencyId, UUID callerId, int page, int size) {
        verifyOwner(agencyId, callerId);
        size = Math.min(Math.max(size, 1), 100);
        return repo.findByAgencyIdOrderByCreatedAtDesc(agencyId, PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public UnreadSummaryDto unreadSummary(UUID agencyId, UUID callerId) {
        verifyOwner(agencyId, callerId);
        return new UnreadSummaryDto(repo.countUnread(agencyId));
    }

    @Transactional
    public void markRead(UUID agencyId, UUID notificationId, UUID callerId) {
        verifyOwner(agencyId, callerId);
        repo.markRead(notificationId, agencyId);
    }

    @Transactional
    public void markAllRead(UUID agencyId, UUID callerId) {
        verifyOwner(agencyId, callerId);
        repo.markAllRead(agencyId);
    }

    private void verifyOwner(UUID agencyId, UUID callerId) {
        JobListingService.UmsAgencyInfo info = jobListingService.umsFetchAgencyInfo(agencyId);
        if (info == null || !callerId.equals(info.ownerUserId())) {
            throw new UnauthorizedAccessException("Only the agency owner can view notifications for this agency");
        }
    }

    private AgencyNotificationDto toDto(AgencyNotification n) {
        return new AgencyNotificationDto(
                n.getNotificationId(), n.getAgencyId(), n.getKind(), n.getJobId(),
                n.getTitle(), n.getBody(), n.getReadAt(), n.getCreatedAt());
    }
}
