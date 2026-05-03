package com.deharri.jlds.worker;

import com.deharri.jlds.enums.AvailabilityStatus;
import com.deharri.jlds.enums.PakistanCity;
import com.deharri.jlds.enums.WorkerType;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.dto.response.JobListingSummaryResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileListResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileResponse;
import com.deharri.jlds.worker.dto.response.WorkerProfileSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkerDiscoveryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WorkerDiscoveryController Unit Tests")
class WorkerDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerProfileService workerProfileService;

    private static final UUID WORKER_ID = UUID.randomUUID();

    // ========================================================================
    // GET /api/v1/workers
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/workers")
    class SearchWorkersTests {

        @Test
        @DisplayName("Should return 200 with paginated worker list")
        void whenSearchWorkers_thenReturn200() throws Exception {
            WorkerProfileSummaryResponse summary = WorkerProfileSummaryResponse.builder()
                    .workerId(WORKER_ID)
                    .username("john_doe")
                    .firstName("John")
                    .lastName("Doe")
                    .workerType(WorkerType.ELECTRICIAN)
                    .workerTypeDisplayName("Electrician")
                    .city(PakistanCity.LAHORE)
                    .cityDisplayName("Lahore")
                    .hourlyRate(new BigDecimal("500"))
                    .availabilityStatus(AvailabilityStatus.AVAILABLE)
                    .isVerified(true)
                    .averageRating(new BigDecimal("4.50"))
                    .totalJobsCompleted(25)
                    .build();

            WorkerProfileListResponse listResponse = WorkerProfileListResponse.builder()
                    .workers(List.of(summary))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(workerProfileService.searchWorkers(any())).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/workers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workers.length()").value(1))
                    .andExpect(jsonPath("$.workers[0].username").value("john_doe"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ========================================================================
    // GET /api/v1/workers/{workerId}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/workers/{workerId}")
    class GetWorkerProfileTests {

        @Test
        @DisplayName("Should return 200 with worker profile")
        void givenValidId_whenGetProfile_thenReturn200() throws Exception {
            WorkerProfileResponse response = WorkerProfileResponse.builder()
                    .workerId(WORKER_ID)
                    .username("john_doe")
                    .firstName("John")
                    .lastName("Doe")
                    .workerType(WorkerType.ELECTRICIAN)
                    .workerTypeDisplayName("Electrician")
                    .city(PakistanCity.LAHORE)
                    .hourlyRate(new BigDecimal("500"))
                    .experienceYears(5)
                    .availabilityStatus(AvailabilityStatus.AVAILABLE)
                    .isVerified(true)
                    .averageRating(new BigDecimal("4.50"))
                    .totalJobsCompleted(25)
                    .build();

            when(workerProfileService.getWorkerProfile(WORKER_ID)).thenReturn(response);

            mockMvc.perform(get("/api/v1/workers/{workerId}", WORKER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workerId").value(WORKER_ID.toString()))
                    .andExpect(jsonPath("$.username").value("john_doe"))
                    .andExpect(jsonPath("$.experienceYears").value(5));
        }
    }

    // ========================================================================
    // GET /api/v1/workers/{workerId}/jobs
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/workers/{workerId}/jobs")
    class GetWorkerJobsTests {

        @Test
        @DisplayName("Should return 200 with worker's completed jobs")
        void whenGetWorkerJobs_thenReturn200() throws Exception {
            JobListingSummaryResponse job = JobListingSummaryResponse.builder()
                    .jobId(UUID.randomUUID())
                    .title("Fixed Wiring")
                    .workerType(WorkerType.ELECTRICIAN)
                    .city(PakistanCity.LAHORE)
                    .status(JobStatus.COMPLETED)
                    .build();

            JobListingListResponse listResponse = JobListingListResponse.builder()
                    .listings(List.of(job))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(workerProfileService.getWorkerCompletedJobs(WORKER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/workers/{workerId}/jobs", WORKER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.listings.length()").value(1))
                    .andExpect(jsonPath("$.listings[0].status").value("COMPLETED"));
        }
    }

    // ========================================================================
    // GET /api/v1/workers/types
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/workers/types")
    class GetWorkerTypesTests {

        @Test
        @DisplayName("Should return 200 with all worker types")
        void whenGetTypes_thenReturn200WithAllTypes() throws Exception {
            mockMvc.perform(get("/api/v1/workers/types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(WorkerType.values().length))
                    .andExpect(jsonPath("$[0].name").exists())
                    .andExpect(jsonPath("$[0].displayName").exists())
                    .andExpect(jsonPath("$[0].description").exists());
        }
    }
}
