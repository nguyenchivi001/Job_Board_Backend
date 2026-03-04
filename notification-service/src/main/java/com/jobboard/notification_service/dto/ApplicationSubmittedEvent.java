package com.jobboard.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSubmittedEvent {
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private Long employerId;
}
