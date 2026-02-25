package com.jobboard.profile_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyProfileRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String description;
    private String website;
    private String logoUrl;
    private String address;
    private Integer employeeSize;
}
