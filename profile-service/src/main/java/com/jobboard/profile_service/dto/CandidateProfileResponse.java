package com.jobboard.profile_service.dto;

import com.jobboard.profile_service.entity.CandidateProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CandidateProfileResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String phone;
    private String bio;
    private String skills;
    private String experience;
    private String education;
    private String resumeUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CandidateProfileResponse from(CandidateProfile profile) {
        return CandidateProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .experience(profile.getExperience())
                .education(profile.getEducation())
                .resumeUrl(profile.getResumeUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
