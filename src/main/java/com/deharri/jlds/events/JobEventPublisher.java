package com.deharri.jlds.events;

import com.deharri.jlds.listing.entity.JobListing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes job lifecycle events to Kafka. Send failures are logged but do not propagate —
 * the originating user action has already committed to the DB and shouldn't fail because of
 * a downstream broker hiccup. Manual safety-net buttons in the FE remain as backstop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobEventPublisher {

    public static final String TOPIC_CONFIRMED = "job.confirmed";
    public static final String TOPIC_CANCELLED = "job.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishConfirmed(JobListing listing) {
        JobLifecycleEvent event = JobLifecycleEvent.builder()
                .type(JobLifecycleEvent.Type.CONFIRMED)
                .jobId(listing.getJobId())
                .consumerId(listing.getConsumerId())
                .assignedWorkerId(listing.getAssignedWorkerId())
                .assignedAgencyId(listing.getAssignedAgencyId())
                .dispatchedWorkerId(listing.getDispatchedWorkerId())
                .occurredAt(Instant.now())
                .build();
        send(TOPIC_CONFIRMED, listing.getJobId().toString(), event);
    }

    public void publishCancelled(JobListing listing) {
        JobLifecycleEvent event = JobLifecycleEvent.builder()
                .type(JobLifecycleEvent.Type.CANCELLED)
                .jobId(listing.getJobId())
                .consumerId(listing.getConsumerId())
                .occurredAt(Instant.now())
                .build();
        send(TOPIC_CANCELLED, listing.getJobId().toString(), event);
    }

    private void send(String topic, String key, JobLifecycleEvent event) {
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish {} for job {}: {}",
                            event.getType(), event.getJobId(), ex.getMessage());
                } else {
                    log.info("Published {} for job {} to topic {}",
                            event.getType(), event.getJobId(), topic);
                }
            });
        } catch (Exception e) {
            log.warn("Synchronous error publishing {} for job {}: {}",
                    event.getType(), event.getJobId(), e.getMessage());
        }
    }
}
