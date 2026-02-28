package com.deharri.jlds.worker;

import com.deharri.jlds.enums.*;
import com.deharri.jlds.worker.entity.WorkerProfile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WorkerProfileSpecification {

    private WorkerProfileSpecification() {}

    public static Specification<WorkerProfile> withFilters(
            WorkerType workerType,
            PakistanCity city,
            PakistanCity serviceCity,
            String skills,
            String search,
            BigDecimal minRating,
            BigDecimal maxHourlyRate,
            BigDecimal maxDailyRate,
            Integer minExperience,
            Boolean available,
            Boolean verified,
            Language language
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (workerType != null) {
                predicates.add(cb.equal(root.get("workerType"), workerType));
            }

            if (city != null) {
                predicates.add(cb.equal(root.get("city"), city));
            }

            if (serviceCity != null) {
                Join<Object, Object> serviceCitiesJoin = root.join("serviceCities");
                predicates.add(cb.equal(serviceCitiesJoin, serviceCity));
            }

            if (skills != null && !skills.isBlank()) {
                // Match any skill from comma-separated list
                String[] skillArray = skills.split(",");
                List<Predicate> skillPredicates = new ArrayList<>();
                Join<Object, Object> skillsJoin = root.join("skills");
                for (String skill : skillArray) {
                    skillPredicates.add(cb.like(cb.lower(skillsJoin.as(String.class)), "%" + skill.trim().toLowerCase() + "%"));
                }
                predicates.add(cb.or(skillPredicates.toArray(new Predicate[0])));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("bio")), pattern)
                ));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }

            if (maxHourlyRate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hourlyRate"), maxHourlyRate));
            }

            if (maxDailyRate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dailyRate"), maxDailyRate));
            }

            if (minExperience != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), minExperience));
            }

            if (Boolean.TRUE.equals(available)) {
                predicates.add(cb.equal(root.get("availabilityStatus"), AvailabilityStatus.AVAILABLE));
            }

            if (Boolean.TRUE.equals(verified)) {
                predicates.add(cb.isTrue(root.get("isVerified")));
            }

            if (language != null) {
                Join<Object, Object> languagesJoin = root.join("languages");
                predicates.add(cb.equal(languagesJoin, language));
            }

            // Ensure distinct results when joining collection tables
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
