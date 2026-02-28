package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UrgencyLevel {
    EMERGENCY("Emergency", 1),
    URGENT("Urgent", 2),
    NORMAL("Normal", 3),
    FLEXIBLE("Flexible", 4);

    private final String displayName;
    private final int priority;
}
