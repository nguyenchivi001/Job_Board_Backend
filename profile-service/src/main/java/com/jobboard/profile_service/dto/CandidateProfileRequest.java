package com.jobboard.profile_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private String bio;
    private String skills;
    private String experience;
    private String education;
    private String resumeUrl;
}
