package com.deharri.jlds.dispute;

import com.deharri.jlds.dispute.dto.response.DisputeResponse;
import com.deharri.jlds.dispute.entity.Dispute.DisputeCategory;
import com.deharri.jlds.dispute.entity.Dispute.DisputeStatus;
import com.deharri.jlds.dispute.entity.Dispute.RaisedByRole;
import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisputeController.class)
@Import(GlobalExceptionHandler.class)
class DisputeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DisputeService disputeService;

    private UUID userId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
    }

    @Test
    void submitDispute_returns201() throws Exception {
        DisputeResponse stub = DisputeResponse.builder()
                .disputeId(UUID.randomUUID()).jobId(jobId)
                .raisedBy(userId).raisedByRole(RaisedByRole.CONSUMER)
                .category(DisputeCategory.PAYMENT).status(DisputeStatus.OPEN)
                .subject("s").description("d")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(disputeService.submitDispute(any(), eq(userId))).thenReturn(stub);

        String body = """
            {
              "jobId": "%s",
              "category": "PAYMENT",
              "subject": "Worker did not arrive",
              "description": "9am booking missed"
            }
            """.formatted(jobId);

        mockMvc.perform(post("/api/v1/disputes")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.raisedByRole").value("CONSUMER"));
    }

    @Test
    void submitDispute_validatesSubjectLength() throws Exception {
        String tooLong = "x".repeat(121);
        String body = """
            {
              "jobId": "%s",
              "category": "PAYMENT",
              "subject": "%s",
              "description": "ok"
            }
            """.formatted(jobId, tooLong);

        mockMvc.perform(post("/api/v1/disputes")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyDisputes_returnsPage() throws Exception {
        Page<DisputeResponse> page = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(disputeService.getMyDisputes(eq(userId), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/disputes/my")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getMyDisputesForJob_returnsList() throws Exception {
        when(disputeService.getMyDisputesForJob(eq(jobId), eq(userId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/listings/{jobId}/disputes/my", jobId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());
    }
}
