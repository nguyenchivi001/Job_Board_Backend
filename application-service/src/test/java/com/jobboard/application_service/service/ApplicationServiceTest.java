package com.jobboard.application_service.service;

import com.jobboard.application_service.client.JobServiceClient;
import com.jobboard.application_service.config.RabbitMQConfig;
import com.jobboard.application_service.dto.ApplicationRequest;
import com.jobboard.application_service.dto.ApplicationResponse;
import com.jobboard.application_service.dto.ApplicationStatusUpdatedEvent;
import com.jobboard.application_service.dto.ApplicationSubmittedEvent;
import com.jobboard.application_service.dto.JobResponse;
import com.jobboard.application_service.dto.UpdateStatusRequest;
import com.jobboard.application_service.entity.Application;
import com.jobboard.application_service.enums.ApplicationStatus;
import com.jobboard.application_service.exception.AlreadyAppliedException;
import com.jobboard.application_service.exception.ApplicationNotFoundException;
import com.jobboard.application_service.exception.JobServiceException;
import com.jobboard.application_service.exception.UnauthorizedException;
import com.jobboard.application_service.repository.ApplicationRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private JobServiceClient jobServiceClient;

    @InjectMocks
    private ApplicationService applicationService;

    private JobResponse openJob(Long jobId, Long employerId) {
        JobResponse job = new JobResponse();
        job.setId(jobId);
        job.setTitle("Dev Job");
        job.setEmployerId(employerId);
        job.setStatus("OPEN");
        return job;
    }

    private Application buildApplication(Long id, Long jobId, Long candidateId) {
        Application app = new Application();
        app.setId(id);
        app.setJobId(jobId);
        app.setCandidateId(candidateId);
        app.setStatus(ApplicationStatus.PENDING);
        return app;
    }

    @Test
    void apply_openJob_notApplied_savesAndPublishesEvent() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);
        request.setCoverLetter("I am interested");

        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 5L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(buildApplication(100L, 1L, 5L));

        ApplicationResponse response = applicationService.apply(5L, request);

        assertThat(response.getJobId()).isEqualTo(1L);
        assertThat(response.getCandidateId()).isEqualTo(5L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_APP_SUBMITTED),
                any(ApplicationSubmittedEvent.class)
        );
    }

    @Test
    void apply_jobNotOpen_throwsJobServiceException() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);

        JobResponse closedJob = openJob(1L, 10L);
        closedJob.setStatus("CLOSED");
        when(jobServiceClient.getJobById(1L)).thenReturn(closedJob);

        assertThatThrownBy(() -> applicationService.apply(5L, request))
                .isInstanceOf(JobServiceException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void apply_alreadyApplied_throwsAlreadyAppliedException() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(1L);

        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(5L, request))
                .isInstanceOf(AlreadyAppliedException.class);
    }

    @Test
    void apply_jobNotFound_throwsJobServiceException() {
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(99L);

        when(jobServiceClient.getJobById(99L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> applicationService.apply(5L, request))
                .isInstanceOf(JobServiceException.class);
    }

    @Test
    void getMyApplications_returnsList() {
        Application app = buildApplication(1L, 1L, 5L);
        when(applicationRepository.findByCandidateId(5L)).thenReturn(List.of(app));

        List<ApplicationResponse> result = applicationService.getMyApplications(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCandidateId()).isEqualTo(5L);
    }

    @Test
    void getApplicationsForJob_ownerEmployer_returnsList() {
        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));
        when(applicationRepository.findByJobId(1L)).thenReturn(List.of(buildApplication(1L, 1L, 5L)));

        List<ApplicationResponse> result = applicationService.getApplicationsForJob(1L, 10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getApplicationsForJob_notOwner_throwsUnauthorizedException() {
        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));

        assertThatThrownBy(() -> applicationService.getApplicationsForJob(1L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateStatus_statusChanged_savesAndPublishesEvent() {
        Application app = buildApplication(1L, 1L, 5L);
        // default status is PENDING, changing to ACCEPTED

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));
        when(applicationRepository.save(app)).thenReturn(app);

        UpdateStatusRequest updateRequest = new UpdateStatusRequest();
        updateRequest.setStatus(ApplicationStatus.ACCEPTED);

        ApplicationResponse response = applicationService.updateStatus(1L, 10L, updateRequest);

        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_APP_STATUS_UPDATED),
                any(ApplicationStatusUpdatedEvent.class)
        );
    }

    @Test
    void updateStatus_sameStatus_savesButNoEvent() {
        Application app = buildApplication(1L, 1L, 5L);
        // default status is PENDING, request is also PENDING

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));
        when(applicationRepository.save(app)).thenReturn(app);

        UpdateStatusRequest updateRequest = new UpdateStatusRequest();
        updateRequest.setStatus(ApplicationStatus.PENDING);

        applicationService.updateStatus(1L, 10L, updateRequest);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void updateStatus_applicationNotFound_throwsApplicationNotFoundException() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateStatusRequest updateRequest = new UpdateStatusRequest();
        updateRequest.setStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.updateStatus(99L, 10L, updateRequest))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void updateStatus_notOwner_throwsUnauthorizedException() {
        Application app = buildApplication(1L, 1L, 5L);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(jobServiceClient.getJobById(1L)).thenReturn(openJob(1L, 10L));

        UpdateStatusRequest updateRequest = new UpdateStatusRequest();
        updateRequest.setStatus(ApplicationStatus.ACCEPTED);

        assertThatThrownBy(() -> applicationService.updateStatus(1L, 99L, updateRequest))
                .isInstanceOf(UnauthorizedException.class);
    }
}
