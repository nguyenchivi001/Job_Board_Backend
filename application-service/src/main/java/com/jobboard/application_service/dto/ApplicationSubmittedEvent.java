package com.jobboard.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSubmittedEvent {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private Long employerId;
}
