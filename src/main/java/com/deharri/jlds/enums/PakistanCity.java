package com.deharri.jlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PakistanCity {
    KARACHI("Karachi", "Sindh"),
    LAHORE("Lahore", "Punjab"),
    ISLAMABAD("Islamabad", "Islamabad Capital Territory"),
    RAWALPINDI("Rawalpindi", "Punjab"),
    FAISALABAD("Faisalabad", "Punjab"),
    MULTAN("Multan", "Punjab"),
    PESHAWAR("Peshawar", "Khyber Pakhtunkhwa"),
    QUETTA("Quetta", "Balochistan"),
    SIALKOT("Sialkot", "Punjab"),
    GUJRANWALA("Gujranwala", "Punjab"),
    HYDERABAD("Hyderabad", "Sindh"),
    ABBOTTABAD("Abbottabad", "Khyber Pakhtunkhwa"),
    BAHAWALPUR("Bahawalpur", "Punjab"),
    SARGODHA("Sargodha", "Punjab"),
    SUKKUR("Sukkur", "Sindh"),
    LARKANA("Larkana", "Sindh"),
    SHEIKHUPURA("Sheikhupura", "Punjab"),
    RAHIM_YAR_KHAN("Rahim Yar Khan", "Punjab"),
    JHANG("Jhang", "Punjab"),
    DERA_GHAZI_KHAN("Dera Ghazi Khan", "Punjab"),
    GUJRAT("Gujrat", "Punjab"),
    SAHIWAL("Sahiwal", "Punjab"),
    WAH_CANTT("Wah Cantt", "Punjab"),
    MARDAN("Mardan", "Khyber Pakhtunkhwa"),
    KASUR("Kasur", "Punjab"),
    MINGORA("Mingora", "Khyber Pakhtunkhwa"),
    NAWABSHAH("Nawabshah", "Sindh"),
    OKARA("Okara", "Punjab"),
    MIRPUR_KHAS("Mirpur Khas", "Sindh"),
    CHINIOT("Chiniot", "Punjab"),
    OTHER("Other", "Other");

    private final String displayName;
    private final String province;
}
