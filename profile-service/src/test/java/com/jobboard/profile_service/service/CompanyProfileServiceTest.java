package com.jobboard.profile_service.service;

import com.jobboard.profile_service.dto.CompanyProfileRequest;
import com.jobboard.profile_service.dto.CompanyProfileResponse;
import com.jobboard.profile_service.entity.CompanyProfile;
import com.jobboard.profile_service.exception.ProfileNotFoundException;
import com.jobboard.profile_service.repository.CompanyProfileRepository;
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
class CompanyProfileServiceTest {

    @Mock
    private CompanyProfileRepository repository;

    @InjectMocks
    private CompanyProfileService companyProfileService;

    private CompanyProfile buildProfile(Long userId, String companyName) {
        CompanyProfile profile = new CompanyProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setCompanyName(companyName);
        profile.setDescription("A tech company");
        profile.setWebsite("https://techcorp.com");
        return profile;
    }

    @Test
    void getByUserId_found_returnsResponse() {
        CompanyProfile profile = buildProfile(10L, "TechCorp");
        when(repository.findByUserId(10L)).thenReturn(Optional.of(profile));

        CompanyProfileResponse response = companyProfileService.getByUserId(10L);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getCompanyName()).isEqualTo("TechCorp");
    }

    @Test
    void getByUserId_notFound_throwsProfileNotFoundException() {
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyProfileService.getByUserId(99L))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void save_newProfile_createsAndReturns() {
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName("NewCorp");
        request.setDescription("A new company");
        request.setWebsite("https://newcorp.com");

        CompanyProfile savedProfile = buildProfile(10L, "NewCorp");
        when(repository.findByUserId(10L)).thenReturn(Optional.empty());
        when(repository.save(any(CompanyProfile.class))).thenReturn(savedProfile);

        CompanyProfileResponse response = companyProfileService.save(10L, request);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getCompanyName()).isEqualTo("NewCorp");
    }

    @Test
    void save_existingProfile_updatesAndReturns() {
        CompanyProfile existingProfile = buildProfile(10L, "TechCorp");
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName("UpdatedCorp");
        request.setDescription("Updated description");

        CompanyProfile updatedProfile = buildProfile(10L, "UpdatedCorp");
        when(repository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));
        when(repository.save(existingProfile)).thenReturn(updatedProfile);

        CompanyProfileResponse response = companyProfileService.save(10L, request);

        assertThat(response.getCompanyName()).isEqualTo("UpdatedCorp");
    }
}
