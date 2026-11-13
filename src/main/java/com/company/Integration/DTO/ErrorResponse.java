package com.company.Integration.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(example = "502") int status,
        @Schema(example = "Bad Gateway") String error,
        @Schema(example = "Weather API failed") String message,
        LocalDateTime timestamp
) {}