package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WorkerType {
    MECHANIC("Mechanic", "Performs mechanical repairs and maintenance"),
    ELECTRICIAN("Electrician", "Handles electrical installations and repairs"),
    PLUMBER("Plumber", "Manages plumbing systems and fixtures"),
    CARPENTER("Carpenter", "Specializes in woodwork and construction"),
    WELDER("Welder", "Performs welding and metal fabrication"),
    PAINTER("Painter", "Handles painting and surface finishing"),
    MASON("Mason", "Works with brick, stone, and concrete"),
    HVAC_TECHNICIAN("HVAC Technician", "Maintains heating, ventilation, and air conditioning systems"),
    GENERAL_LABORER("General Laborer", "Performs general construction and maintenance tasks");

    private final String displayName;
    private final String description;
}
