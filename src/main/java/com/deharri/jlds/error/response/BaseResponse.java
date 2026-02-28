package com.deharri.jlds.error.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Standard error response for API errors")
public class BaseResponse {
    protected final HttpStatus status;
    protected final String message;
    protected final LocalDateTime timestamp;
    protected final String path;
}
