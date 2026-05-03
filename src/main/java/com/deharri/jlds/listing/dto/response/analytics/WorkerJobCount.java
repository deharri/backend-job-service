package com.deharri.jlds.listing.dto.response.analytics;

import java.util.UUID;

public record WorkerJobCount(UUID workerUserId, String workerUsername, long count) {}
