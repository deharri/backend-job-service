package com.deharri.jlds.listing.dto.response.analytics;

public record JobsFunnel(
    long bidsPlaced,
    long bidsAccepted,
    long dispatched,
    long inProgress,
    long completed
) {}
