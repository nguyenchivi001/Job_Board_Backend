package com.jobboard.application_service.service;

import com.jobboard.application_service.client.JobServiceClient;
import com.jobboard.application_service.config.RabbitMQConfig;
import com.jobboard.application_service.dto.*;
import com.jobboard.application_service.entity.Application;
import com.jobboard.application_service.enums.ApplicationStatus;
import com.jobboard.application_service.exception.AlreadyAppliedException;
import com.jobboard.application_service.exception.ApplicationNotFoundException;
import com.jobboard.application_service.exception.JobServiceException;
import com.jobboard.application_service.exception.UnauthorizedException;
import com.jobboard.application_service.repository.ApplicationRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final JobServiceClient jobServiceClient;

    public ApplicationResponse apply(Long candidateId, ApplicationRequest request) {
        // 1. Validate job tồn tại và đang OPEN
        JobResponse job = fetchJob(request.getJobId());
        if (!"OPEN".equals(job.getStatus())) {
            throw new JobServiceException("Job is not open for applications");
        }

        // 2. Kiểm tra đã apply chưa
        if (applicationRepository.existsByJobIdAndCandidateId(request.getJobId(), candidateId)) {
            throw new AlreadyAppliedException("You have already applied for this job");
        }

        // 3. Lưu application
        Application application = new Application();
        application.setJobId(request.getJobId());
        application.setCandidateId(candidateId);
        application.setCoverLetter(request.getCoverLetter());
        Application saved = applicationRepository.save(application);

        // 4. Publish event
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                saved.getId(), saved.getJobId(), saved.getCandidateId(), job.getEmployerId()
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_APP_SUBMITTED, event);
        log.info("Published application.submitted event for applicationId={}", saved.getId());

        return ApplicationResponse.from(saved);
    }

    public List<ApplicationResponse> getMyApplications(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    public List<ApplicationResponse> getApplicationsForJob(Long jobId, Long employerId) {
        // Validate job thuộc về employer này
        JobResponse job = fetchJob(jobId);
        if (!employerId.equals(job.getEmployerId())) {
            throw new UnauthorizedException("You do not own this job");
        }

        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    public ApplicationResponse updateStatus(Long applicationId, Long employerId, UpdateStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + applicationId));

        // Validate job thuộc về employer này
        JobResponse job = fetchJob(application.getJobId());
        if (!employerId.equals(job.getEmployerId())) {
            throw new UnauthorizedException("You do not own this job");
        }

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(request.getStatus());
        Application updated = applicationRepository.save(application);

        // Publish event nếu status thay đổi
        if (!oldStatus.equals(request.getStatus())) {
            ApplicationStatusUpdatedEvent event = new ApplicationStatusUpdatedEvent(
                    updated.getId(), updated.getJobId(), updated.getCandidateId(), updated.getStatus()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_APP_STATUS_UPDATED, event);
            log.info("Published application.status.updated event for applicationId={}, status={}",
                    updated.getId(), updated.getStatus());
        }

        return ApplicationResponse.from(updated);
    }

    private JobResponse fetchJob(Long jobId) {
        try {
            return jobServiceClient.getJobById(jobId);
        } catch (FeignException.NotFound e) {
            throw new JobServiceException("Job not found: " + jobId);
        } catch (Exception e) {
            log.error("Failed to fetch job {}: {}", jobId, e.getMessage());
            throw new JobServiceException("Failed to reach job-service");
        }
    }
}
