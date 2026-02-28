package com.deharri.jlds.listing;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.UrgencyLevel;
import com.deharri.jlds.enums.WorkerType;
import com.deharri.jlds.listing.entity.JobListing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobListingSpecification {

    private JobListingSpecification() {}

    public static Specification<JobListing> withFilters(
            WorkerType workerType,
            PakistanCity city,
            String area,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            UrgencyLevel urgency,
            JobStatus status,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter out expired jobs transparently
            predicates.add(cb.or(
                    cb.notEqual(root.get("status"), JobStatus.OPEN),
                    cb.greaterThan(root.get("expiresAt"), Instant.now())
            ));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (workerType != null) {
                predicates.add(cb.equal(root.get("workerType"), workerType));
            }

            if (city != null) {
                predicates.add(cb.equal(root.get("city"), city));
            }

            if (area != null && !area.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("area")), "%" + area.toLowerCase() + "%"));
            }

            if (budgetMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("budgetMax"), budgetMin));
            }

            if (budgetMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("budgetMin"), budgetMax));
            }

            if (urgency != null) {
                predicates.add(cb.equal(root.get("urgency"), urgency));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
