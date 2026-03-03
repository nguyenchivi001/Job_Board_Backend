package com.jobboard.profile_service.service;

import com.jobboard.profile_service.dto.CandidateProfileRequest;
import com.jobboard.profile_service.dto.CandidateProfileResponse;
import com.jobboard.profile_service.entity.CandidateProfile;
import com.jobboard.profile_service.exception.ProfileNotFoundException;
import com.jobboard.profile_service.repository.CandidateProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateProfileServiceTest {

    @Mock
    private CandidateProfileRepository repository;

    @InjectMocks
    private CandidateProfileService candidateProfileService;

    private CandidateProfile buildProfile(Long userId, String fullName) {
        CandidateProfile profile = new CandidateProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setFullName(fullName);
        profile.setPhone("0123456789");
        profile.setBio("A developer");
        profile.setSkills("Java, Spring");
        return profile;
    }

    @Test
    void getByUserId_found_returnsResponse() {
        CandidateProfile profile = buildProfile(5L, "John Doe");
        when(repository.findByUserId(5L)).thenReturn(Optional.of(profile));

        CandidateProfileResponse response = candidateProfileService.getByUserId(5L);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
    }

    @Test
    void getByUserId_notFound_throwsProfileNotFoundException() {
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateProfileService.getByUserId(99L))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void save_newProfile_createsAndReturns() {
        CandidateProfileRequest request = new CandidateProfileRequest();
        request.setFullName("Jane Doe");
        request.setPhone("0987654321");
        request.setSkills("Python, Django");

        CandidateProfile savedProfile = buildProfile(5L, "Jane Doe");
        when(repository.findByUserId(5L)).thenReturn(Optional.empty());
        when(repository.save(any(CandidateProfile.class))).thenReturn(savedProfile);

        CandidateProfileResponse response = candidateProfileService.save(5L, request);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    void save_existingProfile_updatesAndReturns() {
        CandidateProfile existingProfile = buildProfile(5L, "John Doe");
        CandidateProfileRequest request = new CandidateProfileRequest();
        request.setFullName("Updated Name");
        request.setPhone("0111111111");
        request.setSkills("Go, Kubernetes");

        CandidateProfile updatedProfile = buildProfile(5L, "Updated Name");
        when(repository.findByUserId(5L)).thenReturn(Optional.of(existingProfile));
        when(repository.save(existingProfile)).thenReturn(updatedProfile);

        CandidateProfileResponse response = candidateProfileService.save(5L, request);

        assertThat(response.getFullName()).isEqualTo("Updated Name");
    }
}
