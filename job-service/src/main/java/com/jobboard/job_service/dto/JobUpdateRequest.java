package com.jobboard.job_service.dto;

import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobStatus;
import com.jobboard.job_service.enums.JobType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobUpdateRequest {

    private String title;
    private String description;
    private String company;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private JobType type;
    private JobCategory category;
    private JobStatus status;
    private LocalDateTime deadline;
}
