package com.deharri.jlds.bid.dto.response;

public record BidWinRate(
    long won,
    long pending,
    long rejected,
    long withdrawn,
    long total,
    double winRatePct
) {}
