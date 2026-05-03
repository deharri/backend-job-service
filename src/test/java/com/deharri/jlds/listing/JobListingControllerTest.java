package com.deharri.jlds.listing;

import com.deharri.jlds.enums.*;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.deharri.jlds.listing.dto.request.CreateJobFromOfferRequest;
import com.deharri.jlds.listing.dto.request.CreateJobListingRequest;
import com.deharri.jlds.listing.dto.request.UpdateJobListingRequest;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.dto.response.JobListingResponse;
import com.deharri.jlds.listing.dto.response.JobListingSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobListingController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("JobListingController Unit Tests")
class JobListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobListingService jobListingService;

    private static final UUID CONSUMER_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    private JobListingResponse buildJobResponse() {
        return JobListingResponse.builder()
                .jobId(JOB_ID)
                .consumerId(CONSUMER_ID)
                .consumerUsername("consumer1")
                .title("Fix Wiring")
                .description("Need electrical wiring fixed")
                .workerType(WorkerType.ELECTRICIAN)
                .workerTypeDisplayName("Electrician")
                .city(PakistanCity.LAHORE)
                .cityDisplayName("Lahore")
                .budgetMin(new BigDecimal("5000"))
                .budgetMax(new BigDecimal("10000"))
                .budgetType(BudgetType.FIXED)
                .urgency(UrgencyLevel.NORMAL)
                .status(JobStatus.OPEN)
                .bidCount(0)
                .viewCount(0)
                .createdAt(Instant.now())
                .build();
    }

    // ========================================================================
    // POST /api/v1/listings
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/listings")
    class CreateListingTests {

        @Test
        @DisplayName("Should return 201 when valid listing is created")
        void givenValidRequest_whenCreateListing_thenReturn201() throws Exception {
            CreateJobListingRequest request = CreateJobListingRequest.builder()
                    .title("Fix Wiring")
                    .description("Need electrical wiring fixed")
                    .workerType(WorkerType.ELECTRICIAN)
                    .city(PakistanCity.LAHORE)
                    .budgetMin(new BigDecimal("5000"))
                    .budgetMax(new BigDecimal("10000"))
                    .budgetType(BudgetType.FIXED)
                    .publishImmediately(true)
                    .build();

            JobListingResponse response = buildJobResponse();

            when(jobListingService.createListing(any(CreateJobListingRequest.class), any(UUID.class), anyString()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/listings")
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Fix Wiring"))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @DisplayName("Should return 400 when title is missing")
        void givenMissingTitle_whenCreateListing_thenReturn400() throws Exception {
            CreateJobListingRequest request = CreateJobListingRequest.builder()
                    .description("Need fixing")
                    .workerType(WorkerType.ELECTRICIAN)
                    .city(PakistanCity.LAHORE)
                    .build();

            mockMvc.perform(post("/api/v1/listings")
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // POST /api/v1/listings/from-offer
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/listings/from-offer")
    class CreateJobFromOfferTests {

        @Test
        @DisplayName("Should return 201 when job is created from offer")
        void givenValidOffer_whenCreateJobFromOffer_thenReturn201() throws Exception {
            CreateJobFromOfferRequest request = CreateJobFromOfferRequest.builder()
                    .title("Fix Wiring")
                    .description("Electrical work from chat offer")
                    .workerType(WorkerType.ELECTRICIAN)
                    .city(PakistanCity.LAHORE)
                    .budgetAmount(new BigDecimal("8000"))
                    .consumerId(CONSUMER_ID)
                    .consumerUsername("consumer1")
                    .workerId(WORKER_ID)
                    .workerUsername("worker1")
                    .build();

            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.ASSIGNED);

            when(jobListingService.createJobFromOffer(any(CreateJobFromOfferRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/listings/from-offer")
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()));
        }
    }

    // ========================================================================
    // GET /api/v1/listings
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings")
    class SearchListingsTests {

        @Test
        @DisplayName("Should return 200 with paginated listings")
        void whenSearchListings_thenReturn200WithList() throws Exception {
            JobListingSummaryResponse summary = JobListingSummaryResponse.builder()
                    .jobId(JOB_ID)
                    .title("Fix Wiring")
                    .workerType(WorkerType.ELECTRICIAN)
                    .city(PakistanCity.LAHORE)
                    .status(JobStatus.OPEN)
                    .bidCount(2)
                    .build();

            JobListingListResponse listResponse = JobListingListResponse.builder()
                    .listings(List.of(summary))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(jobListingService.searchListings(any())).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.listings.length()").value(1))
                    .andExpect(jsonPath("$.listings[0].title").value("Fix Wiring"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ========================================================================
    // GET /api/v1/listings/{jobId}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings/{jobId}")
    class GetListingByIdTests {

        @Test
        @DisplayName("Should return 200 with listing details")
        void givenValidId_whenGetListingById_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();

            when(jobListingService.getListingById(JOB_ID)).thenReturn(response);

            mockMvc.perform(get("/api/v1/listings/{jobId}", JOB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jobId").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Fix Wiring"));
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void givenInvalidId_whenGetListingById_thenReturn404() throws Exception {
            UUID invalidId = UUID.randomUUID();

            when(jobListingService.getListingById(invalidId))
                    .thenThrow(new JobNotFoundException("Job not found: " + invalidId));

            mockMvc.perform(get("/api/v1/listings/{jobId}", invalidId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Job not found: " + invalidId));
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}")
    class UpdateListingTests {

        @Test
        @DisplayName("Should return 200 when listing is updated")
        void givenValidUpdate_whenUpdateListing_thenReturn200() throws Exception {
            UpdateJobListingRequest request = UpdateJobListingRequest.builder()
                    .title("Updated Title")
                    .description("Updated description")
                    .build();

            JobListingResponse response = buildJobResponse();
            response.setTitle("Updated Title");

            when(jobListingService.updateListing(eq(JOB_ID), any(UpdateJobListingRequest.class), eq(CONSUMER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @DisplayName("Should return 403 when non-owner tries to update")
        void givenNonOwner_whenUpdateListing_thenReturn403() throws Exception {
            UUID otherId = UUID.randomUUID();
            UpdateJobListingRequest request = UpdateJobListingRequest.builder()
                    .title("Hacked Title")
                    .build();

            when(jobListingService.updateListing(eq(JOB_ID), any(UpdateJobListingRequest.class), eq(otherId)))
                    .thenThrow(new UnauthorizedAccessException("You are not the owner of this listing"));

            mockMvc.perform(put("/api/v1/listings/{jobId}", JOB_ID)
                            .header("X-User-Id", otherId.toString())
                            .header("X-Username", "hacker")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}/publish
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}/publish")
    class PublishListingTests {

        @Test
        @DisplayName("Should return 200 when draft is published")
        void givenDraftListing_whenPublish_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.OPEN);

            when(jobListingService.publishListing(JOB_ID, CONSUMER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}/publish", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}/cancel
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}/cancel")
    class CancelListingTests {

        @Test
        @DisplayName("Should return 200 when listing is cancelled")
        void givenValidListing_whenCancel_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.CANCELLED);

            when(jobListingService.cancelListing(eq(JOB_ID), eq(CONSUMER_ID), anyString()))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}/cancel", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "Changed my mind"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 400 when cancelling completed job")
        void givenCompletedJob_whenCancel_thenReturn400() throws Exception {
            when(jobListingService.cancelListing(eq(JOB_ID), eq(CONSUMER_ID), any()))
                    .thenThrow(new InvalidOperationException("Cannot cancel a completed job"));

            mockMvc.perform(put("/api/v1/listings/{jobId}/cancel", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}/start
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}/start")
    class StartJobTests {

        @Test
        @DisplayName("Should return 200 when assigned worker starts job")
        void givenAssignedWorker_whenStartJob_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.IN_PROGRESS);
            response.setAssignedWorkerId(WORKER_ID);

            when(jobListingService.startJob(JOB_ID, WORKER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}/start", JOB_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}/complete
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}/complete")
    class CompleteJobTests {

        @Test
        @DisplayName("Should return 200 when worker completes job")
        void givenInProgressJob_whenComplete_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.COMPLETED);

            when(jobListingService.completeJob(JOB_ID, WORKER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}/complete", JOB_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    // ========================================================================
    // PUT /api/v1/listings/{jobId}/confirm-completion
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/listings/{jobId}/confirm-completion")
    class ConfirmCompletionTests {

        @Test
        @DisplayName("Should return 200 when consumer confirms completion")
        void givenCompletedJob_whenConfirm_thenReturn200() throws Exception {
            JobListingResponse response = buildJobResponse();
            response.setStatus(JobStatus.COMPLETED);
            response.setConsumerConfirmedAt(Instant.now());

            when(jobListingService.confirmCompletion(JOB_ID, CONSUMER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/listings/{jobId}/confirm-completion", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.consumerConfirmedAt").isNotEmpty());
        }
    }

    // ========================================================================
    // GET /api/v1/listings/my-listings
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings/my-listings")
    class GetMyListingsTests {

        @Test
        @DisplayName("Should return 200 with consumer's listings")
        void whenGetMyListings_thenReturn200() throws Exception {
            JobListingListResponse listResponse = JobListingListResponse.builder()
                    .listings(List.of())
                    .page(0)
                    .size(20)
                    .totalPages(0)
                    .totalElements(0)
                    .build();

            when(jobListingService.getMyListings(CONSUMER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/listings/my-listings")
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ========================================================================
    // GET /api/v1/listings/my-jobs
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings/my-jobs")
    class GetMyJobsTests {

        @Test
        @DisplayName("Should return 200 with worker's assigned jobs")
        void whenGetMyJobs_thenReturn200() throws Exception {
            JobListingSummaryResponse summary = JobListingSummaryResponse.builder()
                    .jobId(JOB_ID)
                    .title("Fix Wiring")
                    .status(JobStatus.IN_PROGRESS)
                    .build();

            JobListingListResponse listResponse = JobListingListResponse.builder()
                    .listings(List.of(summary))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(jobListingService.getMyJobs(WORKER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/listings/my-jobs")
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.listings.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
