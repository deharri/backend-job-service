package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReviewType {
    CONSUMER_TO_WORKER("Consumer to Worker"),
    WORKER_TO_CONSUMER("Worker to Consumer");

    private final String displayName;
}
