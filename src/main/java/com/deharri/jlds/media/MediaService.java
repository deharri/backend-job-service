package com.deharri.jlds.media;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.deharri.jlds.media.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public PresignedUrlResponse generateUploadUrl(String fileName, String contentType, UUID userId) {
        String s3Key = "jobs/" + userId + "/" + UUID.randomUUID() + "/" + fileName;

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 min

        if (contentType != null && !contentType.isBlank()) {
            request.setContentType(contentType);
        }

        String url = amazonS3.generatePresignedUrl(request).toString();

        return PresignedUrlResponse.builder()
                .url(url)
                .s3Key(s3Key)
                .build();
    }

    public PresignedUrlResponse generateDownloadUrl(String s3Key) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, s3Key)
                .withMethod(HttpMethod.GET)
                .withExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)); // 1 hour

        String url = amazonS3.generatePresignedUrl(request).toString();

        return PresignedUrlResponse.builder()
                .url(url)
                .s3Key(s3Key)
                .build();
    }
}
