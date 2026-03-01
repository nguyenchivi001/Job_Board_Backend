package com.jobboard.application_service.dto;

import com.jobboard.application_service.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull(message = "status is required")
    private ApplicationStatus status;
}
