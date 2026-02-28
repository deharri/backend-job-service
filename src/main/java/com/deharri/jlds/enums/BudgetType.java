package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BudgetType {
    FIXED("Fixed"),
    HOURLY("Hourly"),
    DAILY("Daily"),
    NEGOTIABLE("Negotiable");

    private final String displayName;
}
