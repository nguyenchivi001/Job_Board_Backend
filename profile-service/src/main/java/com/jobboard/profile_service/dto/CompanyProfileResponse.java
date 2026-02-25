package com.jobboard.profile_service.dto;

import com.jobboard.profile_service.entity.CompanyProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CompanyProfileResponse {

    private Long id;
    private Long userId;
    private String companyName;
    private String description;
    private String website;
    private String logoUrl;
    private String address;
    private Integer employeeSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyProfileResponse from(CompanyProfile profile) {
        return CompanyProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .companyName(profile.getCompanyName())
                .description(profile.getDescription())
                .website(profile.getWebsite())
                .logoUrl(profile.getLogoUrl())
                .address(profile.getAddress())
                .employeeSize(profile.getEmployeeSize())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
