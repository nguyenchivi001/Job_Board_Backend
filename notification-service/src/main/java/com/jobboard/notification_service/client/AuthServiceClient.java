package com.jobboard.notification_service.client;

import com.jobboard.notification_service.dto.UserEmailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth-service.url}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/{userId}/email")
    UserEmailResponse getUserEmail(@PathVariable("userId") Long userId);
}
