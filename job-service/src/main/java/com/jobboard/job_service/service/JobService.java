package com.jobboard.job_service.service;

import com.jobboard.job_service.config.RabbitMQConfig;
import com.jobboard.job_service.dto.JobCreatedEvent;
import com.jobboard.job_service.dto.JobFiltersResponse;
import com.jobboard.job_service.dto.JobRequest;
import com.jobboard.job_service.dto.JobResponse;
import com.jobboard.job_service.entity.Job;
import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobType;
import com.jobboard.job_service.exception.JobNotFoundException;
import com.jobboard.job_service.exception.UnauthorizedException;
import com.jobboard.job_service.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;

    public Page<JobResponse> search(String title, String location, JobType type, JobCategory category, int page, int size) {
        return jobRepository.search(title, location, type, category, PageRequest.of(page, size))
                .map(JobResponse::from);
    }

    public JobFiltersResponse getFilters() {
        return JobFiltersResponse.builder()
                .locations(jobRepository.findDistinctLocations())
                .categories(Arrays.asList(JobCategory.values()))
                .types(Arrays.asList(JobType.values()))
                .build();
    }

    @Cacheable(value = "jobs", key = "#id")
    public JobResponse getById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        return JobResponse.from(job);
    }

    public JobResponse create(Long employerId, JobRequest request) {
        Job job = Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .company(request.getCompany())
                .location(request.getLocation())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .type(request.getType())
                .category(request.getCategory())
                .status(request.getStatus())
                .employerId(employerId)
                .build();

        Job saved = jobRepository.save(job);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_JOB_CREATED,
                new JobCreatedEvent(saved.getId(), saved.getTitle(), saved.getEmployerId())
        );

        return JobResponse.from(saved);
    }

    @CacheEvict(value = "jobs", key = "#id")
    public JobResponse update(Long id, Long employerId, JobRequest request) {
        Job job = jobRepository.findByIdAndEmployerId(id, employerId)
                .orElseThrow(() -> new UnauthorizedException("You are not authorized to update this job"));

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setType(request.getType());
        job.setCategory(request.getCategory());
        job.setStatus(request.getStatus());

        return JobResponse.from(jobRepository.save(job));
    }

    @CacheEvict(value = "jobs", key = "#id")
    public void delete(Long id, Long employerId) {
        Job job = jobRepository.findByIdAndEmployerId(id, employerId)
                .orElseThrow(() -> new UnauthorizedException("You are not authorized to delete this job"));
        jobRepository.delete(job);
    }
}
