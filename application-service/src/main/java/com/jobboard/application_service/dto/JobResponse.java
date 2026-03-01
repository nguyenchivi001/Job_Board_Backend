package com.jobboard.application_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobResponse {

    private Long id;
    private String title;
    private Long employerId;
    private String status;
}
