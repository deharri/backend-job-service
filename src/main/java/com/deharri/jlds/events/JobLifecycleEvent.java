package com.deharri.jlds.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Single envelope used on both {@code job.confirmed} and {@code job.cancelled} topics.
 * Disambiguated by {@link #type}. Worker/agency fields are populated only on CONFIRMED events.
 *
 * <p>Each consuming service has its own JSON-compatible copy of this class; the JSON
 * structure (field names) is the contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobLifecycleEvent {

    public enum Type { CONFIRMED, CANCELLED }

    private Type type;
    private UUID jobId;
    private UUID consumerId;

    /** Set when the job had a directly-assigned worker (no agency). Null otherwise. */
    private UUID assignedWorkerId;
    /** Set when the job was assigned to an agency. Null otherwise. */
    private UUID assignedAgencyId;
    /** Set when the agency dispatched a specific worker. Null otherwise. */
    private UUID dispatchedWorkerId;

    private Instant occurredAt;
}
