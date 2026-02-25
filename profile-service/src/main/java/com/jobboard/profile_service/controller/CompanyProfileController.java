package com.jobboard.profile_service.controller;

import com.jobboard.profile_service.dto.CompanyProfileRequest;
import com.jobboard.profile_service.dto.CompanyProfileResponse;
import com.jobboard.profile_service.service.CompanyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/company")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService service;

    @GetMapping("/{userId}")
    public ResponseEntity<CompanyProfileResponse> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PutMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<CompanyProfileResponse> saveProfile(
            @RequestHeader("X-User-Id") Long currentUserId,
            @Valid @RequestBody CompanyProfileRequest request) {

        return ResponseEntity.ok(service.save(currentUserId, request));
    }
}
