package com.vector.onboarding.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String githubId;

    @Column(nullable = false)
    private String username;

    private String email;

    private String profileImageUrl;

    @Column(length = 500)
    private String githubAccessToken;

    private String teamCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public User(String githubId, String username, String email, String profileImageUrl, String githubAccessToken, Role role) {
        this.githubId = githubId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.githubAccessToken = githubAccessToken;
        this.role = role;
    }

    public User update(String username, String email, String profileImageUrl, String githubAccessToken) {
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.githubAccessToken = githubAccessToken;
        return this;
    }

    public void updateTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }
}
