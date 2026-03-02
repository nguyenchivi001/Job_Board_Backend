package com.jobboard.job_service.dto;

import com.jobboard.job_service.entity.Job;
import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobStatus;
import com.jobboard.job_service.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse implements Serializable {

    private Long id;
    private String title;
    private String description;
    private String company;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private JobType type;
    private JobCategory category;
    private JobStatus status;
    private Long employerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .company(job.getCompany())
                .location(job.getLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .type(job.getType())
                .category(job.getCategory())
                .status(job.getStatus())
                .employerId(job.getEmployerId())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
