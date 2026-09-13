package com.sage.teachingassistant.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Starts a new run. */
public record StartRunRequest(

        @NotBlank(message = "workflowKey must not be blank")
        String workflowKey,

        @NotBlank(message = "message must not be blank")
        String message
) {
}
