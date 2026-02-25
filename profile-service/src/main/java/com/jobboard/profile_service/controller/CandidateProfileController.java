package com.jobboard.profile_service.controller;

import com.jobboard.profile_service.dto.CandidateProfileRequest;
import com.jobboard.profile_service.dto.CandidateProfileResponse;
import com.jobboard.profile_service.service.CandidateProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles/candidate")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService service;

    @GetMapping("/{userId}")
    public ResponseEntity<CandidateProfileResponse> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PutMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<CandidateProfileResponse> saveProfile(
            @RequestHeader("X-User-Id") Long currentUserId,
            @Valid @RequestBody CandidateProfileRequest request) {

        return ResponseEntity.ok(service.save(currentUserId, request));
    }
}
