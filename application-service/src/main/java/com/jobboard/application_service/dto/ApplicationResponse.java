package com.jobboard.application_service.dto;

import com.jobboard.application_service.entity.Application;
import com.jobboard.application_service.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private Long candidateId;
    private ApplicationStatus status;
    private String coverLetter;
    private LocalDateTime appliedAt;

    public static ApplicationResponse from(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setJobId(application.getJobId());
        response.setCandidateId(application.getCandidateId());
        response.setStatus(application.getStatus());
        response.setCoverLetter(application.getCoverLetter());
        response.setAppliedAt(application.getAppliedAt());
        return response;
    }
}
