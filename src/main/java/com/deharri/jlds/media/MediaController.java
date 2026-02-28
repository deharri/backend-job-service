package com.deharri.jlds.media;

import com.deharri.jlds.media.dto.PresignedUrlResponse;
import com.deharri.jlds.media.dto.UploadUrlRequest;
import com.deharri.jlds.util.HeaderExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "S3 presigned URL generation for file uploads/downloads")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload-url")
    @Operation(summary = "Get presigned S3 upload URL")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = HeaderExtractor.getUserId(httpRequest);
        return ResponseEntity.ok(mediaService.generateUploadUrl(
                request.getFileName(), request.getContentType(), userId));
    }

    @GetMapping("/download-url")
    @Operation(summary = "Get presigned S3 download URL")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @RequestParam String s3Key) {
        return ResponseEntity.ok(mediaService.generateDownloadUrl(s3Key));
    }
}
