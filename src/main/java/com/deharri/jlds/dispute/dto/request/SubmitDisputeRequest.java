package com.deharri.jlds.dispute.dto.request;

import com.deharri.jlds.dispute.entity.Dispute.DisputeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitDisputeRequest {

    @NotNull(message = "jobId is required")
    private UUID jobId;

    @NotNull(message = "category is required")
    private DisputeCategory category;

    @NotBlank(message = "subject is required")
    @Size(max = 120, message = "subject must be at most 120 characters")
    private String subject;

    @NotBlank(message = "description is required")
    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;
}
