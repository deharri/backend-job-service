package com.deharri.jlds.error.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Schema(name = "ValidationErrorResponse", description = "Response containing field-level validation errors")
public class FieldsValidationExceptionResponse extends BaseResponse {

    @Schema(description = "Map of field names to their validation error messages")
    private final Map<String, String> errors;

    public FieldsValidationExceptionResponse(HttpStatus status, String message, Map<String, String> errors, String path) {
        super(status, message, LocalDateTime.now(), path);
        this.errors = errors;
    }
}
