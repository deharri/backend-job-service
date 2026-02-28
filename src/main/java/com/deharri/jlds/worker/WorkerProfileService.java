package com.deharri.jlds.worker;

import com.deharri.jlds.enums.*;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.listing.mapper.JobListingMapper;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.worker.dto.request.WorkerSearchFilter;
import com.deharri.jlds.worker.dto.response.WorkerProfileListResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileResponse;
import com.deharri.jlds.worker.entity.WorkerProfile;
import com.deharri.jlds.worker.mapper.WorkerProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerProfileService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerProfileMapper workerProfileMapper;
    private final JobListingRepository jobListingRepository;
    private final JobListingMapper jobListingMapper;
    private final RestTemplate restTemplate;

    private static final String UMS_BASE_URL = "http://user-mgmt-service/api/v1/internal/workers";

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
            "rating", "averageRating",
            "experience", "experienceYears",
            "hourlyRate", "hourlyRate",
            "jobsCompleted", "totalJobsCompleted"
    );

    @Value("${worker-sync.ttl-minutes:60}")
    private int syncTtlMinutes;

    @Transactional(readOnly = true)
    public WorkerProfileListResponse searchWorkers(WorkerSearchFilter filter) {
        int size = Math.min(Math.max(filter.getSize(), 1), 50);
        String sortField = SORT_FIELD_MAP.getOrDefault(filter.getSortBy(), "averageRating");
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(filter.getPage(), size, Sort.by(direction, sortField));

        Specification<WorkerProfile> spec = WorkerProfileSpecification.withFilters(
                filter.getWorkerType(),
                filter.getCity(),
                filter.getServiceCity(),
                filter.getSkills(),
                filter.getSearch(),
                filter.getMinRating(),
                filter.getMaxHourlyRate(),
                filter.getMaxDailyRate(),
                filter.getMinExperience(),
                filter.getAvailable(),
                filter.getVerified(),
                filter.getLanguage()
        );

        Page<WorkerProfile> page = workerProfileRepository.findAll(spec, pageable);

        return WorkerProfileListResponse.builder()
                .workers(page.getContent().stream().map(workerProfileMapper::toSummaryResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public WorkerProfileResponse getWorkerProfile(UUID workerId) {
        WorkerProfile profile = workerProfileRepository.findById(workerId)
                .orElseThrow(() -> new JobNotFoundException("Worker profile not found: " + workerId));

        // Trigger async refresh if stale
        if (isStale(profile)) {
            refreshWorkerProfileAsync(workerId);
        }

        return workerProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getWorkerCompletedJobs(UUID workerId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobListing> jobs = jobListingRepository.findCompletedJobsByWorkerId(workerId, pageable);

        return JobListingListResponse.builder()
                .listings(jobs.getContent().stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(jobs.getNumber())
                .size(jobs.getSize())
                .totalElements(jobs.getTotalElements())
                .totalPages(jobs.getTotalPages())
                .build();
    }

    // --- Sync Logic ---

    @Transactional
    public int syncAllWorkers() {
        log.info("Starting full worker profile sync from UMS...");
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    UMS_BASE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> workers = response.getBody();
            if (workers == null || workers.isEmpty()) {
                log.warn("No workers returned from UMS sync");
                return 0;
            }

            int count = 0;
            for (Map<String, Object> workerData : workers) {
                try {
                    upsertWorkerProfile(workerData);
                    count++;
                } catch (Exception e) {
                    log.error("Failed to sync worker: {}", workerData.get("workerId"), e);
                }
            }

            log.info("Full sync completed. {} workers synced.", count);
            return count;
        } catch (Exception e) {
            log.error("Failed to sync workers from UMS: {}", e.getMessage());
            return 0;
        }
    }

    @Transactional
    public void syncSingleWorker(Map<String, Object> workerData) {
        try {
            upsertWorkerProfile(workerData);
            log.info("Synced worker profile: {}", workerData.get("workerId"));
        } catch (Exception e) {
            log.error("Failed to sync single worker: {}", e.getMessage());
        }
    }

    @Async
    public void refreshWorkerProfileAsync(UUID workerId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    UMS_BASE_URL + "/" + workerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> workerData = response.getBody();
            if (workerData != null) {
                upsertWorkerProfile(workerData);
                log.debug("Async refreshed worker profile: {}", workerId);
            }
        } catch (Exception e) {
            log.warn("Failed to async refresh worker {}: {}", workerId, e.getMessage());
        }
    }

    // --- Internal ---

    @SuppressWarnings("unchecked")
    private void upsertWorkerProfile(Map<String, Object> data) {
        UUID workerId = UUID.fromString((String) data.get("workerId"));

        WorkerProfile profile = workerProfileRepository.findById(workerId)
                .orElse(WorkerProfile.builder().workerId(workerId).build());

        profile.setUserId(UUID.fromString((String) data.get("userId")));
        profile.setUsername((String) data.get("username"));
        profile.setFirstName((String) data.get("firstName"));
        profile.setLastName((String) data.get("lastName"));
        profile.setProfilePicturePath((String) data.get("profilePicturePath"));

        if (data.get("workerType") != null) {
            profile.setWorkerType(WorkerType.valueOf((String) data.get("workerType")));
        }

        if (data.get("skills") != null) {
            profile.setSkills((List<String>) data.get("skills"));
        }

        profile.setBio((String) data.get("bio"));

        if (data.get("experienceYears") != null) {
            profile.setExperienceYears((Integer) data.get("experienceYears"));
        }

        if (data.get("hourlyRate") != null) {
            profile.setHourlyRate(new java.math.BigDecimal(data.get("hourlyRate").toString()));
        }
        if (data.get("dailyRate") != null) {
            profile.setDailyRate(new java.math.BigDecimal(data.get("dailyRate").toString()));
        }

        if (data.get("city") != null) {
            profile.setCity(PakistanCity.valueOf((String) data.get("city")));
        }
        profile.setArea((String) data.get("area"));

        if (data.get("serviceCities") != null) {
            List<String> cityStrings = (List<String>) data.get("serviceCities");
            profile.setServiceCities(cityStrings.stream().map(PakistanCity::valueOf).toList());
        }

        if (data.get("languages") != null) {
            List<String> langStrings = (List<String>) data.get("languages");
            profile.setLanguages(langStrings.stream().map(Language::valueOf).toList());
        }

        if (data.get("availabilityStatus") != null) {
            profile.setAvailabilityStatus(AvailabilityStatus.valueOf((String) data.get("availabilityStatus")));
        }

        if (data.get("isVerified") != null) {
            profile.setIsVerified((Boolean) data.get("isVerified"));
        }

        if (data.get("averageRating") != null) {
            profile.setAverageRating(new java.math.BigDecimal(data.get("averageRating").toString()));
        }

        if (data.get("totalJobsCompleted") != null) {
            profile.setTotalJobsCompleted((Integer) data.get("totalJobsCompleted"));
        }

        profile.setAgencyName((String) data.get("agencyName"));
        profile.setLastSyncedAt(Instant.now());

        workerProfileRepository.save(profile);
    }

    private boolean isStale(WorkerProfile profile) {
        if (profile.getLastSyncedAt() == null) return true;
        return profile.getLastSyncedAt().isBefore(Instant.now().minus(syncTtlMinutes, ChronoUnit.MINUTES));
    }
}
