package com.jobboard.job_service.controller;

import com.jobboard.job_service.dto.JobFiltersResponse;
import com.jobboard.job_service.dto.JobRequest;
import com.jobboard.job_service.dto.JobUpdateRequest;
import com.jobboard.job_service.dto.JobResponse;
import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobType;
import com.jobboard.job_service.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<Page<JobResponse>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) JobCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.search(title, location, type, category, page, size));
    }

    @GetMapping("/filters")
    public ResponseEntity<JobFiltersResponse> getFilters() {
        return ResponseEntity.ok(jobService.getFilters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobResponse> create(
            @RequestHeader("X-User-Id") Long employerId,
            @Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(employerId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobResponse> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId,
            @RequestBody JobUpdateRequest request) {
        return ResponseEntity.ok(jobService.update(id, employerId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long employerId) {
        jobService.delete(id, employerId);
        return ResponseEntity.noContent().build();
    }
}
