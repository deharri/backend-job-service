package com.deharri.jlds.listing.dto.response.analytics;

import java.time.Instant;

public record TimeBucket(Instant bucket, long count) {}
