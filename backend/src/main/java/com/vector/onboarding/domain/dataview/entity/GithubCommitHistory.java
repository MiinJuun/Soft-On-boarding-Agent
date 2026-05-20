package com.vector.onboarding.domain.dataview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_commit_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubCommitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repositoryUrl;

    @Column(nullable = false)
    private String commitHash;

    @Column(columnDefinition = "TEXT")
    private String commitMessage;

    @Column(nullable = false)
    private String authorName;

    @Column(columnDefinition = "TEXT")
    private String diffContent;

    private LocalDateTime committedAt;

    @Builder
    public GithubCommitHistory(String repositoryUrl, String commitHash, String commitMessage, String authorName, String diffContent, LocalDateTime committedAt) {
        this.repositoryUrl = repositoryUrl;
        this.commitHash = commitHash;
        this.commitMessage = commitMessage;
        this.authorName = authorName;
        this.diffContent = diffContent;
        this.committedAt = committedAt;
    }
}
