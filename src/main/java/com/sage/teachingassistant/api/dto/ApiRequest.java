package com.sage.teachingassistant.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Incoming request body. */
public record ApiRequest(

        @NotBlank(message = "message must not be blank")
        String message
) {
}
