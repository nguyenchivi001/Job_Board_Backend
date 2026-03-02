package com.jobboard.notification_service.listener;

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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_JOB_CREATED)
    public void handleJobCreated(JobCreatedEvent event) {
        log.info("Received job.created event: jobId={}, title={}", event.getId(), event.getTitle());

        emailService.sendEmail(
                "employer-" + event.getEmployerId() + "@jobboard.local",
                "Your job has been posted: " + event.getTitle(),
                "job-created",
                Map.of(
                        "jobId", event.getId(),
                        "jobTitle", event.getTitle(),
                        "employerId", event.getEmployerId()
                )
        );
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APP_SUBMITTED)
    public void handleApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Received application.submitted event: applicationId={}, jobId={}", event.getApplicationId(), event.getJobId());

        // Notify employer
        emailService.sendEmail(
                "employer-" + event.getEmployerId() + "@jobboard.local",
                "New application received for job #" + event.getJobId(),
                "application-submitted",
                Map.of(
                        "applicationId", event.getApplicationId(),
                        "jobId", event.getJobId(),
                        "candidateId", event.getCandidateId()
                )
        );
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APP_STATUS_UPDATED)
    public void handleApplicationStatusUpdated(ApplicationStatusUpdatedEvent event) {
        log.info("Received application.status.updated event: applicationId={}, status={}", event.getApplicationId(), event.getStatus());

        // Notify candidate
        emailService.sendEmail(
                "candidate-" + event.getCandidateId() + "@jobboard.local",
                "Your application status has been updated: " + event.getStatus(),
                "application-status-updated",
                Map.of(
                        "applicationId", event.getApplicationId(),
                        "jobId", event.getJobId(),
                        "status", event.getStatus()
                )
        );
    }
}
