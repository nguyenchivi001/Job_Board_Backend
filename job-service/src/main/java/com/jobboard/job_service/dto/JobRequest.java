package com.jobboard.job_service.dto;

import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobStatus;
import com.jobboard.job_service.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String company;

    private String location;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    @NotNull
    private JobType type;

    private JobCategory category;

    private JobStatus status = JobStatus.OPEN;

    private LocalDateTime deadline;
}
