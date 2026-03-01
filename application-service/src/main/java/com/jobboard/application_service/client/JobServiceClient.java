package com.jobboard.application_service.client;

import com.jobboard.application_service.dto.JobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", url = "${job-service.url}")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    JobResponse getJobById(@PathVariable("id") Long id);
}
