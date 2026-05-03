package com.deharri.jlds.review;

import com.deharri.jlds.enums.ReviewType;
import com.deharri.jlds.error.exception.InvalidOperationException;
import com.deharri.jlds.error.exception.JobNotFoundException;
import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.deharri.jlds.review.dto.request.CreateReviewRequest;
import com.deharri.jlds.review.dto.response.RatingSummary;
import com.deharri.jlds.review.dto.response.ReviewListResponse;
import com.deharri.jlds.review.dto.response.ReviewResponse;
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

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReviewController Unit Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    private static final UUID CONSUMER_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();

    private ReviewResponse buildReviewResponse(ReviewType type) {
        return ReviewResponse.builder()
                .reviewId(REVIEW_ID)
                .jobId(JOB_ID)
                .reviewerId(type == ReviewType.CONSUMER_TO_WORKER ? CONSUMER_ID : WORKER_ID)
                .revieweeId(type == ReviewType.CONSUMER_TO_WORKER ? WORKER_ID : CONSUMER_ID)
                .reviewType(type)
                .rating(5)
                .comment("Great work!")
                .qualityRating(type == ReviewType.CONSUMER_TO_WORKER ? 5 : null)
                .communicationRating(4)
                .createdAt(Instant.now())
                .build();
    }

    // ========================================================================
    // POST /api/v1/listings/{jobId}/reviews
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/listings/{jobId}/reviews")
    class CreateReviewTests {

        @Test
        @DisplayName("Should return 201 when review is created")
        void givenValidReview_whenCreate_thenReturn201() throws Exception {
            CreateReviewRequest request = CreateReviewRequest.builder()
                    .rating(5)
                    .comment("Great work!")
                    .qualityRating(5)
                    .communicationRating(4)
                    .punctualityRating(5)
                    .valueRating(4)
                    .build();

            ReviewResponse response = buildReviewResponse(ReviewType.CONSUMER_TO_WORKER);

            when(reviewService.createReview(eq(JOB_ID), any(CreateReviewRequest.class), eq(CONSUMER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/listings/{jobId}/reviews", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
                    .andExpect(jsonPath("$.rating").value(5))
                    .andExpect(jsonPath("$.reviewType").value("CONSUMER_TO_WORKER"));
        }

        @Test
        @DisplayName("Should return 400 when rating is missing")
        void givenMissingRating_whenCreate_thenReturn400() throws Exception {
            CreateReviewRequest request = CreateReviewRequest.builder()
                    .comment("No rating provided")
                    .build();

            mockMvc.perform(post("/api/v1/listings/{jobId}/reviews", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when job is not completed")
        void givenIncompleteJob_whenCreate_thenReturn400() throws Exception {
            CreateReviewRequest request = CreateReviewRequest.builder()
                    .rating(5)
                    .build();

            when(reviewService.createReview(eq(JOB_ID), any(CreateReviewRequest.class), eq(CONSUMER_ID)))
                    .thenThrow(new InvalidOperationException("Job must be completed and confirmed before reviewing"));

            mockMvc.perform(post("/api/v1/listings/{jobId}/reviews", JOB_ID)
                            .header("X-User-Id", CONSUMER_ID.toString())
                            .header("X-Username", "consumer1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // GET /api/v1/listings/{jobId}/reviews
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/listings/{jobId}/reviews")
    class GetReviewsForJobTests {

        @Test
        @DisplayName("Should return 200 with reviews for job")
        void givenValidJob_whenGetReviews_thenReturn200() throws Exception {
            List<ReviewResponse> reviews = List.of(
                    buildReviewResponse(ReviewType.CONSUMER_TO_WORKER),
                    buildReviewResponse(ReviewType.WORKER_TO_CONSUMER)
            );

            when(reviewService.getReviewsForJob(JOB_ID)).thenReturn(reviews);

            mockMvc.perform(get("/api/v1/listings/{jobId}/reviews", JOB_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void givenInvalidJob_whenGetReviews_thenReturn404() throws Exception {
            UUID invalidId = UUID.randomUUID();

            when(reviewService.getReviewsForJob(invalidId))
                    .thenThrow(new JobNotFoundException("Job not found: " + invalidId));

            mockMvc.perform(get("/api/v1/listings/{jobId}/reviews", invalidId))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // GET /api/v1/reviews/worker/{workerId}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/reviews/worker/{workerId}")
    class GetWorkerReviewsTests {

        @Test
        @DisplayName("Should return 200 with paginated worker reviews")
        void whenGetWorkerReviews_thenReturn200() throws Exception {
            ReviewListResponse listResponse = ReviewListResponse.builder()
                    .reviews(List.of(buildReviewResponse(ReviewType.CONSUMER_TO_WORKER)))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(reviewService.getWorkerReviews(WORKER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/reviews/worker/{workerId}", WORKER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviews.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ========================================================================
    // GET /api/v1/reviews/worker/{workerId}/summary
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/reviews/worker/{workerId}/summary")
    class GetWorkerRatingSummaryTests {

        @Test
        @DisplayName("Should return 200 with worker rating summary")
        void whenGetWorkerSummary_thenReturn200() throws Exception {
            RatingSummary summary = RatingSummary.builder()
                    .userId(WORKER_ID)
                    .averageRating(new BigDecimal("4.50"))
                    .totalReviews(10L)
                    .averageQualityRating(new BigDecimal("4.60"))
                    .averagePunctualityRating(new BigDecimal("4.40"))
                    .averageCommunicationRating(new BigDecimal("4.80"))
                    .averageValueRating(new BigDecimal("4.30"))
                    .build();

            when(reviewService.getWorkerRatingSummary(WORKER_ID)).thenReturn(summary);

            mockMvc.perform(get("/api/v1/reviews/worker/{workerId}/summary", WORKER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.50))
                    .andExpect(jsonPath("$.totalReviews").value(10));
        }
    }

    // ========================================================================
    // GET /api/v1/reviews/consumer/{consumerId}
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/reviews/consumer/{consumerId}")
    class GetConsumerReviewsTests {

        @Test
        @DisplayName("Should return 200 with paginated consumer reviews")
        void whenGetConsumerReviews_thenReturn200() throws Exception {
            ReviewListResponse listResponse = ReviewListResponse.builder()
                    .reviews(List.of(buildReviewResponse(ReviewType.WORKER_TO_CONSUMER)))
                    .page(0)
                    .size(20)
                    .totalPages(1)
                    .totalElements(1)
                    .build();

            when(reviewService.getConsumerReviews(CONSUMER_ID, 0, 20)).thenReturn(listResponse);

            mockMvc.perform(get("/api/v1/reviews/consumer/{consumerId}", CONSUMER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviews.length()").value(1));
        }
    }

    // ========================================================================
    // GET /api/v1/reviews/consumer/{consumerId}/summary
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/reviews/consumer/{consumerId}/summary")
    class GetConsumerRatingSummaryTests {

        @Test
        @DisplayName("Should return 200 with consumer rating summary")
        void whenGetConsumerSummary_thenReturn200() throws Exception {
            RatingSummary summary = RatingSummary.builder()
                    .userId(CONSUMER_ID)
                    .averageRating(new BigDecimal("4.20"))
                    .totalReviews(5L)
                    .averageCommunicationRating(new BigDecimal("4.00"))
                    .averageReliabilityRating(new BigDecimal("4.40"))
                    .build();

            when(reviewService.getConsumerRatingSummary(CONSUMER_ID)).thenReturn(summary);

            mockMvc.perform(get("/api/v1/reviews/consumer/{consumerId}/summary", CONSUMER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.20))
                    .andExpect(jsonPath("$.totalReviews").value(5));
        }
    }
}
