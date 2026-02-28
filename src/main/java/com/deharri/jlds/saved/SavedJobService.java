package com.deharri.jlds.saved;

import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.listing.mapper.JobListingMapper;
import com.deharri.jlds.saved.entity.SavedJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingMapper jobListingMapper;

    @Transactional
    public void saveJob(UUID userId, UUID jobId) {
        if (!jobListingRepository.existsById(jobId)) {
            throw new JobNotFoundException("Job listing not found: " + jobId);
        }
        if (savedJobRepository.existsByUserIdAndJobId(userId, jobId)) {
            throw new InvalidOperationException("Job already saved");
        }
        savedJobRepository.save(SavedJob.builder().userId(userId).jobId(jobId).build());
    }

    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        SavedJob saved = savedJobRepository.findByUserIdAndJobId(userId, jobId)
                .orElseThrow(() -> new JobNotFoundException("Saved job not found"));
        savedJobRepository.delete(saved);
    }

    @Transactional(readOnly = true)
    public JobListingListResponse getSavedJobs(UUID userId, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size);
        Page<SavedJob> savedJobs = savedJobRepository.findByUserIdOrderBySavedAtDesc(userId, pageable);

        List<UUID> jobIds = savedJobs.getContent().stream().map(SavedJob::getJobId).toList();
        List<JobListing> listings = jobListingRepository.findAllById(jobIds);

        return JobListingListResponse.builder()
                .listings(listings.stream().map(jobListingMapper::toSummaryResponse).toList())
                .page(savedJobs.getNumber())
                .size(savedJobs.getSize())
                .totalElements(savedJobs.getTotalElements())
                .totalPages(savedJobs.getTotalPages())
                .build();
    }
}
