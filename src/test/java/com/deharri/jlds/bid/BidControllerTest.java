package com.deharri.jlds.bid;

import com.deharri.jlds.bid.dto.request.CreateBidRequest;
import com.deharri.jlds.bid.dto.request.UpdateBidRequest;
import com.deharri.jlds.bid.dto.response.BidListResponse;
import com.deharri.jlds.bid.dto.response.BidResponse;
import com.deharri.jlds.enums.BidStatus;
import com.deharri.jlds.enums.BudgetType;
import com.deharri.jlds.enums.WorkerType;
import com.deharri.jlds.error.exception.BidNotFoundException;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.UnauthorizedAccessException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BidController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("BidController Unit Tests")
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BidService bidService;

    private static final UUID CONSUMER_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID BID_ID = UUID.randomUUID();

    private BidResponse buildBidResponse() {
        return BidResponse.builder()
                .bidId(BID_ID)
                .jobId(JOB_ID)
                .workerId(WORKER_ID)
                .workerUsername("worker1")
                .workerFirstName("Ali")
                .workerLastName("Khan")
                .workerWorkerType(WorkerType.ELECTRICIAN)
                .proposedAmount(new BigDecimal("7000"))
                .proposedRateType(BudgetType.FIXED)
                .coverMessage("I can fix this")
                .estimatedDays(3)
                .status(BidStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    // ========================================================================
    // POST /api/v1/listings/{jobId}/bids
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/listings/{jobId}/bids")
    class PlaceBidTests {

        @Test
        @DisplayName("Should return 201 when bid is placed successfully")
        void givenValidBid_whenPlaceBid_thenReturn201() throws Exception {
            CreateBidRequest request = CreateBidRequest.builder()
                    .proposedAmount(new BigDecimal("7000"))
                    .proposedRateType(BudgetType.FIXED)
                    .coverMessage("I can fix this")
                    .estimatedDays(3)
                    .build();

            BidResponse response = buildBidResponse();

            when(bidService.placeBid(eq(JOB_ID), any(CreateBidRequest.class), eq(WORKER_ID), eq("worker1")))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/listings/{jobId}/bids", JOB_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bidId").value(BID_ID.toString()))
                    .andExpect(jsonPath("$.proposedAmount").value(7000))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 400 when proposedAmount is missing")
        void givenMissingAmount_whenPlaceBid_thenReturn400() throws Exception {
            CreateBidRequest request = CreateBidRequest.builder()
                    .coverMessage("I can fix this")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{jobId}/bids", JOB_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when consumer bids on own job")
        void givenConsumerBidsOwnJob_whenPlaceBid_thenReturn400() throws Exception {
            CreateBidRequest request = CreateBidRequest.builder()
                    .proposedAmount(new BigDecimal("5000"))
                    .build();

            when(bidService.placeBid(eq(JOB_ID), any(CreateBidRequest.class), eq(CONSUMER_ID), eq("consumer1")))
                    .thenThrow(new InvalidOperationException("Cannot bid on your own job"));

            mockMvc.perform(post("/api/v1/listings/{jobId}/bids", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // GET /api/v1/listings/{jobId}/bids
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings/{jobId}/bids")
    class GetBidsForJobTests {

        @Test
        @DisplayName("Should return 200 with paginated bids for job owner")
        void givenJobOwner_whenGetBids_thenReturn200() throws Exception {
            BidListResponse listResponse = BidListResponse.builder()
                    .bids(List.of(buildBidResponse()))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(bidService.getBidsForJob(JOB_ID, CONSUMER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/listings/{jobId}/bids", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bids.length()").value(1))
                    .andExpect(jsonPath("$.bids[0].workerUsername").value("worker1"));
        }
    }

    // ========================================================================
    // GET /api/v1/bids/{bidId}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/bids/{bidId}")
    class GetBidByIdTests {

        @Test
        @DisplayName("Should return 200 with bid details")
        void givenValidBidId_whenGetBid_thenReturn200() throws Exception {
            BidResponse response = buildBidResponse();

            when(bidService.getBidById(BID_ID, WORKER_ID)).thenReturn(response);

            mockMvc.perform(get("/api/v1/bids/{bidId}", BID_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bidId").value(BID_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when bid not found")
        void givenInvalidBidId_whenGetBid_thenReturn404() throws Exception {
            UUID invalidId = UUID.randomUUID();

            when(bidService.getBidById(invalidId, WORKER_ID))
                    .thenThrow(new BidNotFoundException("Bid not found: " + invalidId));

            mockMvc.perform(get("/api/v1/bids/{bidId}", invalidId)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // PUT /api/v1/bids/{bidId}
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/bids/{bidId}")
    class UpdateBidTests {

        @Test
        @DisplayName("Should return 200 when bid is updated")
        void givenValidUpdate_whenUpdateBid_thenReturn200() throws Exception {
            UpdateBidRequest request = UpdateBidRequest.builder()
                    .proposedAmount(new BigDecimal("8000"))
                    .coverMessage("Updated message")
                    .build();

            BidResponse response = buildBidResponse();
            response.setProposedAmount(new BigDecimal("8000"));

            when(bidService.updateBid(eq(BID_ID), any(UpdateBidRequest.class), eq(WORKER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/bids/{bidId}", BID_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proposedAmount").value(8000));
        }
    }

    // ========================================================================
    // PUT /api/v1/bids/{bidId}/withdraw
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/bids/{bidId}/withdraw")
    class WithdrawBidTests {

        @Test
        @DisplayName("Should return 200 when bid is withdrawn")
        void givenPendingBid_whenWithdraw_thenReturn200() throws Exception {
            BidResponse response = buildBidResponse();
            response.setStatus(BidStatus.WITHDRAWN);

            when(bidService.withdrawBid(BID_ID, WORKER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/bids/{bidId}/withdraw", BID_ID)
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("WITHDRAWN"));
        }
    }

    // ========================================================================
    // PUT /api/v1/bids/{bidId}/accept
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/bids/{bidId}/accept")
    class AcceptBidTests {

        @Test
        @DisplayName("Should return 200 when consumer accepts bid")
        void givenValidBid_whenAccept_thenReturn200() throws Exception {
            BidResponse response = buildBidResponse();
            response.setStatus(BidStatus.ACCEPTED);

            when(bidService.acceptBid(BID_ID, CONSUMER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/bids/{bidId}/accept", BID_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }
    }

    // ========================================================================
    // PUT /api/v1/bids/{bidId}/reject
    // ========================================================================

    @Nested
    @DisplayName("PUT /api/v1/bids/{bidId}/reject")
    class RejectBidTests {

        @Test
        @DisplayName("Should return 200 when consumer rejects bid")
        void givenPendingBid_whenReject_thenReturn200() throws Exception {
            BidResponse response = buildBidResponse();
            response.setStatus(BidStatus.REJECTED);

            when(bidService.rejectBid(BID_ID, CONSUMER_ID)).thenReturn(response);

            mockMvc.perform(put("/api/v1/bids/{bidId}/reject", BID_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }
    }

    // ========================================================================
    // GET /api/v1/bids/my-bids
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/bids/my-bids")
    class GetMyBidsTests {

        @Test
        @DisplayName("Should return 200 with worker's bids")
        void whenGetMyBids_thenReturn200() throws Exception {
            BidListResponse listResponse = BidListResponse.builder()
                    .bids(List.of(buildBidResponse()))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(bidService.getMyBids(WORKER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/bids/my-bids")
                            .header("X-User-Id", WORKER_ID.toString())
                            .header("X-Username", "worker1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bids.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
