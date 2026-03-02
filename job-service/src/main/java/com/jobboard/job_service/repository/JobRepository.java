package com.jobboard.job_service.repository;

import com.jobboard.job_service.entity.Job;
import com.jobboard.job_service.enums.JobCategory;
import com.jobboard.job_service.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%',:title,'%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%',:location,'%'))) AND " +
           "(:type IS NULL OR j.type = :type) AND " +
           "(:category IS NULL OR j.category = :category) AND " +
           "j.status = 'OPEN'")
    Page<Job> search(@Param("title") String title,
                     @Param("location") String location,
                     @Param("type") JobType type,
                     @Param("category") JobCategory category,
                     Pageable pageable);

    Optional<Job> findByIdAndEmployerId(Long id, Long employerId);

    @Query("SELECT DISTINCT j.location FROM Job j WHERE j.location IS NOT NULL ORDER BY j.location")
    List<String> findDistinctLocations();

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.status = 'CLOSED' WHERE j.status = 'OPEN' AND j.deadline < :now")
    int closeExpiredJobs(@Param("now") LocalDateTime now);
}
