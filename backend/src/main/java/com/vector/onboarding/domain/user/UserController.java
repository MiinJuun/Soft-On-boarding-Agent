package com.vector.onboarding.domain.user;

import com.vector.onboarding.domain.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new UserResponseDto(user));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<com.vector.onboarding.domain.user.dto.UserProfileResponseDto> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.valueOf(userDetails.getUsername());
        com.vector.onboarding.domain.user.dto.UserProfileResponseDto response = userService.getUserProfile(userId);

        return ResponseEntity.ok(response);
    }
}
