package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReviewType {
    CONSUMER_TO_WORKER("Consumer to Worker"),
    WORKER_TO_CONSUMER("Worker to Consumer"),
    CONSUMER_TO_AGENCY("Consumer to Agency"),
    AGENCY_TO_CONSUMER("Agency to Consumer");

    private final String displayName;
}
