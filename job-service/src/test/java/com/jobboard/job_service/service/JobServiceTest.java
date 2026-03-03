package com.jobboard.job_service.service;

import com.jobboard.job_service.config.RabbitMQConfig;
import com.jobboard.job_service.dto.JobCreatedEvent;
import com.jobboard.job_service.dto.JobFiltersResponse;
import com.jobboard.job_service.dto.JobRequest;
import com.jobboard.job_service.dto.JobResponse;
import com.jobboard.job_service.dto.JobUpdateRequest;
import com.jobboard.job_service.entity.Job;
import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobStatus;
import com.jobboard.job_service.enums.JobType;
import com.jobboard.job_service.exception.JobNotFoundException;
import com.jobboard.job_service.exception.UnauthorizedException;
import com.jobboard.job_service.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private JobService jobService;

    private Job buildJob(Long id, Long employerId) {
        return Job.builder()
                .id(id)
                .title("Software Engineer")
                .description("Job description")
                .company("TechCorp")
                .location("Hanoi")
                .type(JobType.FULL_TIME)
                .category(JobCategory.IT)
                .status(JobStatus.OPEN)
                .employerId(employerId)
                .build();
    }

    @Test
    void search_returnsPageOfResults() {
        Job job = buildJob(1L, 10L);
        Page<Job> page = new PageImpl<>(List.of(job));
        when(jobRepository.search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        Page<JobResponse> result = jobService.search(null, null, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Software Engineer");
    }

    @Test
    void getFilters_returnsLocationsAndEnums() {
        when(jobRepository.findDistinctLocations()).thenReturn(List.of("Hanoi", "HCM"));

        JobFiltersResponse filters = jobService.getFilters();

        assertThat(filters.getLocations()).containsExactly("Hanoi", "HCM");
        assertThat(filters.getCategories()).containsExactlyInAnyOrder(JobCategory.values());
        assertThat(filters.getTypes()).containsExactlyInAnyOrder(JobType.values());
    }

    @Test
    void getById_existingId_returnsJobResponse() {
        Job job = buildJob(1L, 10L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        JobResponse result = jobService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Software Engineer");
    }

    @Test
    void getById_notFound_throwsJobNotFoundException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getById(99L))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void create_savesJobAndPublishesEvent() {
        JobRequest request = new JobRequest();
        request.setTitle("Backend Dev");
        request.setDescription("Description");
        request.setCompany("Tech Inc");
        request.setType(JobType.FULL_TIME);
        request.setStatus(JobStatus.OPEN);

        Job saved = buildJob(1L, 5L);
        saved.setTitle("Backend Dev");
        when(jobRepository.save(any(Job.class))).thenReturn(saved);

        JobResponse result = jobService.create(5L, request);

        assertThat(result.getEmployerId()).isEqualTo(5L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq(RabbitMQConfig.ROUTING_KEY_JOB_CREATED),
                any(JobCreatedEvent.class)
        );
    }

    @Test
    void update_ownerUpdatesFields_returnsUpdated() {
        Job job = buildJob(1L, 10L);
        when(jobRepository.findByIdAndEmployerId(1L, 10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);

        JobUpdateRequest updateRequest = new JobUpdateRequest();
        updateRequest.setTitle("Updated Title");

        JobResponse result = jobService.update(1L, 10L, updateRequest);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(jobRepository).save(job);
    }

    @Test
    void update_notOwner_throwsUnauthorizedException() {
        when(jobRepository.findByIdAndEmployerId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.update(1L, 99L, new JobUpdateRequest()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void delete_ownerDeletesJob() {
        Job job = buildJob(1L, 10L);
        when(jobRepository.findByIdAndEmployerId(1L, 10L)).thenReturn(Optional.of(job));

        jobService.delete(1L, 10L);

        verify(jobRepository).delete(job);
    }

    @Test
    void delete_notOwner_throwsUnauthorizedException() {
        when(jobRepository.findByIdAndEmployerId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.delete(1L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }
}
