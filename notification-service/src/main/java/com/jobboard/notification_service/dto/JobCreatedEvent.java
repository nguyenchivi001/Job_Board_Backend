package com.jobboard.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCreatedEvent {
    private Long id;
    private String title;
    private Long employerId;
}
