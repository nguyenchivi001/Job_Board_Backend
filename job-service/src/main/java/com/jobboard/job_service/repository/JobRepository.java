package com.jobboard.job_service.repository;

import com.jobboard.job_service.entity.Job;
import com.jobboard.job_service.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%',:title,'%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%',:location,'%'))) AND " +
           "(:type IS NULL OR j.type = :type) AND j.status = 'OPEN'")
    Page<Job> search(@Param("title") String title,
                     @Param("location") String location,
                     @Param("type") JobType type,
                     Pageable pageable);

    Optional<Job> findByIdAndEmployerId(Long id, Long employerId);
}
