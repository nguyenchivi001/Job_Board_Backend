package com.jobboard.application_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequest {

    @NotNull(message = "jobId is required")
    private Long jobId;

    private String coverLetter;
}
