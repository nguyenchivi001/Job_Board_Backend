package com.jobboard.application_service.dto;

import com.jobboard.application_service.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdatedEvent {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private ApplicationStatus status;
}
