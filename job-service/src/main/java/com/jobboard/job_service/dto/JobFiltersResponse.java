package com.jobboard.job_service.dto;

import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobFiltersResponse {
    private List<String> locations;
    private List<JobCategory> categories;
    private List<JobType> types;
}
