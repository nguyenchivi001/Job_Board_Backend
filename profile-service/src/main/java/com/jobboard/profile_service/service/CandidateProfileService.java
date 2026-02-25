package com.jobboard.profile_service.service;

import com.jobboard.profile_service.dto.CandidateProfileRequest;
import com.jobboard.profile_service.dto.CandidateProfileResponse;
import com.jobboard.profile_service.entity.CandidateProfile;
import com.jobboard.profile_service.exception.ProfileNotFoundException;
import com.jobboard.profile_service.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository repository;

    public CandidateProfileResponse getByUserId(Long userId) {
        CandidateProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Candidate profile not found for userId: " + userId));
        return CandidateProfileResponse.from(profile);
    }

    public CandidateProfileResponse save(Long currentUserId, CandidateProfileRequest request) {
        CandidateProfile profile = repository.findByUserId(currentUserId)
                .orElse(new CandidateProfile());

        profile.setUserId(currentUserId);
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setBio(request.getBio());
        profile.setSkills(request.getSkills());
        profile.setExperience(request.getExperience());
        profile.setEducation(request.getEducation());
        profile.setResumeUrl(request.getResumeUrl());

        return CandidateProfileResponse.from(repository.save(profile));
    }
}
