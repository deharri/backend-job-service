package com.deharri.jlds.review;

import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.ReviewType;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.listing.JobListingRepository;
import com.deharri.jlds.listing.JobListingService;
import com.deharri.jlds.listing.entity.JobListing;
import com.deharri.jlds.review.dto.request.CreateReviewRequest;
import com.deharri.jlds.review.entity.JobReview;
import com.deharri.jlds.review.mapper.ReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService — direction inference")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewMapper reviewMapper;
    @Mock private JobListingService jobListingService;
    @Mock private JobListingRepository jobListingRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private ReviewService service;

    private static final UUID JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONSUMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WORKER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID AGENCY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID AGENCY_OWNER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTSIDER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private JobListing baseJob;
    private CreateReviewRequest request;

    @BeforeEach
    void setup() {
        baseJob = JobListing.builder()
                .jobId(JOB_ID)
                .consumerId(CONSUMER_ID)
                .status(JobStatus.COMPLETED)
                .consumerConfirmedAt(Instant.now())
                .build();
        request = new CreateReviewRequest();
        request.setRating(5);

        lenient().when(reviewMapper.toEntity(any())).thenAnswer(inv -> {
            JobReview r = new JobReview();
            r.setRating(((CreateReviewRequest) inv.getArgument(0)).getRating());
            return r;
        });
        lenient().when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(reviewRepository.findByJobIdAndReviewType(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("CONSUMER_TO_WORKER: consumer reviewing on a worker job")
    void consumerReviewingWorker() {
        baseJob.setAssignedWorkerId(WORKER_ID);
        when(jobListingService.findListingOrThrow(JOB_ID)).thenReturn(baseJob);

        service.createReview(JOB_ID, request, CONSUMER_ID);

        ArgumentCaptor<JobReview> cap = ArgumentCaptor.forClass(JobReview.class);
        verify(reviewRepository).save(cap.capture());
        assertThat(cap.getValue().getReviewType()).isEqualTo(ReviewType.CONSUMER_TO_WORKER);
        assertThat(cap.getValue().getReviewerId()).isEqualTo(CONSUMER_ID);
        assertThat(cap.getValue().getRevieweeId()).isEqualTo(WORKER_ID);
    }

    @Test
    @DisplayName("CONSUMER_TO_AGENCY: consumer reviewing on an agency job")
    void consumerReviewingAgency() {
        baseJob.setAssignedAgencyId(AGENCY_ID);
        when(jobListingService.findListingOrThrow(JOB_ID)).thenReturn(baseJob);

        service.createReview(JOB_ID, request, CONSUMER_ID);

        ArgumentCaptor<JobReview> cap = ArgumentCaptor.forClass(JobReview.class);
        verify(reviewRepository).save(cap.capture());
        assertThat(cap.getValue().getReviewType()).isEqualTo(ReviewType.CONSUMER_TO_AGENCY);
        assertThat(cap.getValue().getReviewerId()).isEqualTo(CONSUMER_ID);
        assertThat(cap.getValue().getRevieweeId()).isEqualTo(AGENCY_ID);
    }

    @Test
    @DisplayName("WORKER_TO_CONSUMER: assigned worker reviewing the consumer")
    void workerReviewingConsumer() {
        baseJob.setAssignedWorkerId(WORKER_ID);
        when(jobListingService.findListingOrThrow(JOB_ID)).thenReturn(baseJob);

        service.createReview(JOB_ID, request, WORKER_ID);

        ArgumentCaptor<JobReview> cap = ArgumentCaptor.forClass(JobReview.class);
        verify(reviewRepository).save(cap.capture());
        assertThat(cap.getValue().getReviewType()).isEqualTo(ReviewType.WORKER_TO_CONSUMER);
        assertThat(cap.getValue().getReviewerId()).isEqualTo(WORKER_ID);
        assertThat(cap.getValue().getRevieweeId()).isEqualTo(CONSUMER_ID);
    }

    @Test
    @DisplayName("AGENCY_TO_CONSUMER: agency owner verified via UMS, reviewerId stored as agency UUID")
    void agencyReviewingConsumer_ownerVerified() {
        baseJob.setAssignedAgencyId(AGENCY_ID);
        when(jobListingService.findListingOrThrow(JOB_ID)).thenReturn(baseJob);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("ownerUserId", AGENCY_OWNER_ID.toString())));

        service.createReview(JOB_ID, request, AGENCY_OWNER_ID);

        ArgumentCaptor<JobReview> cap = ArgumentCaptor.forClass(JobReview.class);
        verify(reviewRepository).save(cap.capture());
        assertThat(cap.getValue().getReviewType()).isEqualTo(ReviewType.AGENCY_TO_CONSUMER);
        assertThat(cap.getValue().getReviewerId())
                .as("AGENCY_TO_CONSUMER reviewerId should be the agency UUID, not the human owner")
                .isEqualTo(AGENCY_ID);
        assertThat(cap.getValue().getRevieweeId()).isEqualTo(CONSUMER_ID);
    }

    @Test
    @DisplayName("Non-owner caller on agency job → 403")
    void agencyJob_nonOwner_throws() {
        baseJob.setAssignedAgencyId(AGENCY_ID);
        when(jobListingService.findListingOrThrow(JOB_ID)).thenReturn(baseJob);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("ownerUserId", AGENCY_OWNER_ID.toString())));

        assertThatThrownBy(() -> service.createReview(JOB_ID, request, OUTSIDER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}
