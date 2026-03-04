package com.jobboard.notification_service.listener;

import com.jobboard.notification_service.client.AuthServiceClient;
import com.jobboard.notification_service.config.RabbitMQConfig;
import com.jobboard.notification_service.dto.ApplicationStatusUpdatedEvent;
import com.jobboard.notification_service.dto.ApplicationSubmittedEvent;
import com.jobboard.notification_service.dto.JobCreatedEvent;
import com.jobboard.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_JOB_CREATED)
    public void handleJobCreated(JobCreatedEvent event) {
        log.info("Received job.created event: jobId={}, title={}", event.getId(), event.getTitle());
        try {
            String employerEmail = authServiceClient.getUserEmail(event.getEmployerId()).getEmail();
            emailService.sendEmail(
                    employerEmail,
                    "Your job has been posted: " + event.getTitle(),
                    "job-created",
                    Map.of(
                            "jobId", event.getId(),
                            "jobTitle", event.getTitle(),
                            "employerId", event.getEmployerId()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to process job.created event for jobId={}: {}", event.getId(), e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APP_SUBMITTED)
    public void handleApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Received application.submitted event: applicationId={}, jobId={}", event.getApplicationId(), event.getJobId());
        try {
            String employerEmail = authServiceClient.getUserEmail(event.getEmployerId()).getEmail();
            emailService.sendEmail(
                    employerEmail,
                    "New application received for job #" + event.getJobId(),
                    "application-submitted",
                    Map.of(
                            "applicationId", event.getApplicationId(),
                            "jobId", event.getJobId(),
                            "candidateId", event.getCandidateId()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to process application.submitted event for applicationId={}: {}", event.getApplicationId(), e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APP_STATUS_UPDATED)
    public void handleApplicationStatusUpdated(ApplicationStatusUpdatedEvent event) {
        log.info("Received application.status.updated event: applicationId={}, status={}", event.getApplicationId(), event.getStatus());
        try {
            String candidateEmail = authServiceClient.getUserEmail(event.getCandidateId()).getEmail();
            emailService.sendEmail(
                    candidateEmail,
                    "Your application status has been updated: " + event.getStatus(),
                    "application-status-updated",
                    Map.of(
                            "applicationId", event.getApplicationId(),
                            "jobId", event.getJobId(),
                            "status", event.getStatus()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to process application.status.updated event for applicationId={}: {}", event.getApplicationId(), e.getMessage());
        }
    }
}
