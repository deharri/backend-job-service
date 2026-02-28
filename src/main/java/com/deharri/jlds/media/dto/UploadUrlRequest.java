package com.deharri.jlds.media.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    private String contentType;
}
