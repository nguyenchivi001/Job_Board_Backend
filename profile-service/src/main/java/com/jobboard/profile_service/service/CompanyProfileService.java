package com.jobboard.profile_service.service;

import com.jobboard.profile_service.dto.CompanyProfileRequest;
import com.jobboard.profile_service.dto.CompanyProfileResponse;
import com.jobboard.profile_service.entity.CompanyProfile;
import com.jobboard.profile_service.exception.ProfileNotFoundException;
import com.jobboard.profile_service.repository.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyProfileService {

    private final CompanyProfileRepository repository;

    public CompanyProfileResponse getByUserId(Long userId) {
        CompanyProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Company profile not found for userId: " + userId));
        return CompanyProfileResponse.from(profile);
    }

    public CompanyProfileResponse save(Long currentUserId, CompanyProfileRequest request) {
        CompanyProfile profile = repository.findByUserId(currentUserId)
                .orElse(new CompanyProfile());

        profile.setUserId(currentUserId);
        profile.setCompanyName(request.getCompanyName());
        profile.setDescription(request.getDescription());
        profile.setWebsite(request.getWebsite());
        profile.setLogoUrl(request.getLogoUrl());
        profile.setAddress(request.getAddress());
        profile.setEmployeeSize(request.getEmployeeSize());

        return CompanyProfileResponse.from(repository.save(profile));
    }
}
