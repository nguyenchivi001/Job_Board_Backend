package com.jobboard.profile_service.repository;

import com.jobboard.profile_service.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
    Optional<CompanyProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
