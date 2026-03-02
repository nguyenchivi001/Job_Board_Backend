package com.jobboard.job_service.scheduler;

import com.jobboard.job_service.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void closeExpiredJobs() {
        int count = jobRepository.closeExpiredJobs(LocalDateTime.now());
        log.info("Closed {} expired jobs", count);
    }
}
