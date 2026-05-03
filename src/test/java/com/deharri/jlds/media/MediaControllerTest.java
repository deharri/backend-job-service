package com.deharri.jlds.media;

import com.deharri.jlds.error.handler.GlobalExceptionHandler;
import com.deharri.jlds.media.dto.PresignedUrlResponse;
import com.deharri.jlds.media.dto.UploadUrlRequest;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MediaController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MediaController Unit Tests")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MediaService mediaService;

    private static final UUID USER_ID = UUID.randomUUID();

    // ========================================================================
    // POST /api/v1/media/upload-url
    // ========================================================================

    @Nested
    @DisplayName("POST /api/v1/media/upload-url")
    class GetUploadUrlTests {

        @Test
        @DisplayName("Should return 200 with presigned upload URL")
        void givenValidRequest_whenGetUploadUrl_thenReturn200() throws Exception {
            UploadUrlRequest request = new UploadUrlRequest();
            request.setFileName("photo.jpg");
            request.setContentType("image/jpeg");

            PresignedUrlResponse response = PresignedUrlResponse.builder()
                    .url("https://s3.amazonaws.com/bucket/jobs/upload-url")
                    .s3Key("jobs/" + USER_ID + "/uuid/photo.jpg")
                    .build();

            when(mediaService.generateUploadUrl(eq("photo.jpg"), eq("image/jpeg"), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/media/upload-url")
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").isNotEmpty())
                    .andExpect(jsonPath("$.s3Key").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 400 when fileName is missing")
        void givenMissingFileName_whenGetUploadUrl_thenReturn400() throws Exception {
            UploadUrlRequest request = new UploadUrlRequest();

            mockMvc.perform(post("/api/v1/media/upload-url")
                            .header("X-User-Id", USER_ID.toString())
                            .header("X-Username", "user1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========================================================================
    // GET /api/v1/media/download-url
    // ========================================================================

    @Nested
    @DisplayName("GET /api/v1/media/download-url")
    class GetDownloadUrlTests {

        @Test
        @DisplayName("Should return 200 with presigned download URL")
        void givenValidS3Key_whenGetDownloadUrl_thenReturn200() throws Exception {
            String s3Key = "jobs/" + USER_ID + "/uuid/photo.jpg";

            PresignedUrlResponse response = PresignedUrlResponse.builder()
                    .url("https://s3.amazonaws.com/bucket/" + s3Key)
                    .s3Key(s3Key)
                    .build();

            when(mediaService.generateDownloadUrl(s3Key)).thenReturn(response);

            mockMvc.perform(get("/api/v1/media/download-url")
                            .param("s3Key", s3Key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").isNotEmpty())
                    .andExpect(jsonPath("$.s3Key").value(s3Key));
        }
    }
}
