package com.deharri.jlds.worker;

import com.deharri.jlds.error.handler.GlobalExceptionHandler;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkerSyncController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WorkerSyncController Unit Tests")
class WorkerSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkerProfileService workerProfileService;

    // ========================================================================
    // POST /api/v1/internal/workers/sync-all
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/internal/workers/sync-all")
    class SyncAllTests {

        @Test
        @DisplayName("Should return 200 with sync count")
        void whenSyncAll_thenReturn200WithCount() throws Exception {
            when(workerProfileService.syncAllWorkers()).thenReturn(15);

            mockMvc.perform(post("/api/v1/internal/workers/sync-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncedCount").value(15))
                    .andExpect(jsonPath("$.status").value("completed"));

            verify(workerProfileService).syncAllWorkers();
        }
    }

    // ========================================================================
    // POST /api/v1/internal/workers/sync
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/internal/workers/sync")
    class SyncSingleWorkerTests {

        @Test
        @DisplayName("Should return 200 when single worker is synced")
        void givenWorkerData_whenSync_thenReturn200() throws Exception {
            Map<String, Object> workerData = Map.of(
                    "workerId", "some-uuid",
                    "username", "john_doe",
                    "firstName", "John",
                    "workerType", "ELECTRICIAN"
            );

            doNothing().when(workerProfileService).syncSingleWorker(any());

            mockMvc.perform(post("/api/v1/internal/workers/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(workerData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("synced"));

            verify(workerProfileService).syncSingleWorker(any());
        }
    }
}
