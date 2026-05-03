package com.deharri.jlds.dispute.dto.response;

import com.deharri.jlds.dispute.entity.Dispute.DisputeCategory;
import com.deharri.jlds.dispute.entity.Dispute.DisputeStatus;
import com.deharri.jlds.dispute.entity.Dispute.RaisedByRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeResponse {
    private UUID disputeId;
    private UUID jobId;
    private UUID raisedBy;
    private RaisedByRole raisedByRole;
    private DisputeCategory category;
    private String subject;
    private String description;
    private DisputeStatus status;
    private String adminNote;
    private Instant createdAt;
    private Instant updatedAt;
}
