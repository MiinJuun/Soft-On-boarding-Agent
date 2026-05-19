package com.vector.onboarding.domain.user.dto;

import com.vector.onboarding.domain.user.User;
import lombok.Getter;

@Getter
public class UserResponseDto {
    private Long id;
    private String githubId;
    private String username;
    private String email;
    private String profileImageUrl;
    private String teamCode;
    private String role;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.githubId = user.getGithubId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.profileImageUrl = user.getProfileImageUrl();
        this.teamCode = user.getTeamCode();
        this.role = user.getRole().name();
    }
}
