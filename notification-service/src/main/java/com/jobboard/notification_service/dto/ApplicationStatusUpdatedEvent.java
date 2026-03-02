package com.jobboard.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdatedEvent {
    private Long applicationId;
    private Long jobId;
    private Long candidateId;
    private String status;
}
