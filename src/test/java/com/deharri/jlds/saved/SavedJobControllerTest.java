package com.deharri.jlds.saved;

import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.deharri.jlds.listing.dto.response.JobListingListResponse;
import com.deharri.jlds.listing.dto.response.JobListingSummaryResponse;
import com.deharri.jlds.enums.JobStatus;
import com.deharri.jlds.enums.WorkerType;
import com.deharri.jlds.enums.PakistanCity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavedJobController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("SavedJobController Unit Tests")
class SavedJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SavedJobService savedJobService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();

    // ========================================================================
    // POST /api/v1/listings/{jobId}/save
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/listings/{jobId}/save")
    class SaveJobTests {

        @Test
        @DisplayName("Should return 201 when job is saved")
        void givenValidJob_whenSave_thenReturn201() throws Exception {
            doNothing().when(savedJobService).saveJob(USER_ID, JOB_ID);

            mockMvc.perform(post("/api/v1/listings/{jobId}/save", JOB_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1"))
                    .andExpect(status().isCreated());

            verify(savedJobService).saveJob(USER_ID, JOB_ID);
        }

        @Test
        @DisplayName("Should return 400 when job already saved")
        void givenAlreadySaved_whenSave_thenReturn400() throws Exception {
            doThrow(new InvalidOperationException("Job already saved"))
                    .when(savedJobService).saveJob(USER_ID, JOB_ID);

            mockMvc.perform(post("/api/v1/listings/{jobId}/save", JOB_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // DELETE /api/v1/listings/{jobId}/save
    // ========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/listings/{jobId}/save")
    class UnsaveJobTests {

        @Test
        @DisplayName("Should return 204 when job is unsaved")
        void givenSavedJob_whenUnsave_thenReturn204() throws Exception {
            doNothing().when(savedJobService).unsaveJob(USER_ID, JOB_ID);

            mockMvc.perform(delete("/api/v1/listings/{jobId}/save", JOB_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1"))
                    .andExpect(status().isNoContent());

            verify(savedJobService).unsaveJob(USER_ID, JOB_ID);
        }

        @Test
        @DisplayName("Should return 404 when saved job not found")
        void givenNotSaved_whenUnsave_thenReturn404() throws Exception {
            doThrow(new JobNotFoundException("Saved job not found"))
                    .when(savedJobService).unsaveJob(USER_ID, JOB_ID);

            mockMvc.perform(delete("/api/v1/listings/{jobId}/save", JOB_ID)
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // GET /api/v1/saved-jobs
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/saved-jobs")
    class GetSavedJobsTests {

        @Test
        @DisplayName("Should return 200 with saved job listings")
        void whenGetSavedJobs_thenReturn200() throws Exception {
            JobListingSummaryResponse summary = JobListingSummaryResponse.builder()
                    .jobId(JOB_ID)
                    .title("Fix Plumbing")
                    .workerType(WorkerType.PLUMBER)
                    .city(PakistanCity.KARACHI)
                    .status(JobStatus.OPEN)
                    .build();

            JobListingListResponse listResponse = JobListingListResponse.builder()
                    .listings(List.of(summary))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(savedJobService.getSavedJobs(USER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/saved-jobs")
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.listings.length()").value(1))
                    .andExpect(jsonPath("$.listings[0].title").value("Fix Plumbing"));
        }
    }
}
