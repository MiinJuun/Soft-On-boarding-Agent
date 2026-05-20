package com.vector.onboarding.domain.user.dto;

import com.vector.onboarding.domain.space.Space;
import com.vector.onboarding.domain.user.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserProfileResponseDto {

    private String userName;
    private String userEmail;
    private String role; // jobRole from SpaceMember
    private TeamInfo teamInfo;

    public UserProfileResponseDto(User user, Space space, String jobRole) {
        this.userName = user.getUsername();
        this.userEmail = user.getEmail();
        this.role = jobRole != null ? jobRole : "팀 없음";
        
        if (space != null) {
            this.teamInfo = new TeamInfo(
                    space.getName(),
                    space.getTeamCode(),
                    space.getRepoUrl(),
                    space.getCreatedAt()
            );
        } else {
            this.teamInfo = null;
        }
    }

    @Getter
    public static class TeamInfo {
        private String teamName;
        private String teamCode;
        private String repoUrl;
        private LocalDateTime createdAt;

        public TeamInfo(String teamName, String teamCode, String repoUrl, LocalDateTime createdAt) {
            this.teamName = teamName;
            this.teamCode = teamCode;
            this.repoUrl = repoUrl;
            this.createdAt = createdAt;
        }
    }
}
