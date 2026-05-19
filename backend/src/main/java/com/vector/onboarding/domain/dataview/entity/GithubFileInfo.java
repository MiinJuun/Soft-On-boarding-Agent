package com.vector.onboarding.domain.dataview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_file_infos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubFileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repositoryUrl;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String lastCommitHash;

    private LocalDateTime lastSyncedAt;

    @Builder
    public GithubFileInfo(String repositoryUrl, String filePath, String fileName, String lastCommitHash, LocalDateTime lastSyncedAt) {
        this.repositoryUrl = repositoryUrl;
        this.filePath = filePath;
        this.fileName = fileName;
        this.lastCommitHash = lastCommitHash;
        this.lastSyncedAt = lastSyncedAt;
    }

    public void updateCommitHash(String commitHash) {
        this.lastCommitHash = commitHash;
        this.lastSyncedAt = LocalDateTime.now();
    }
}
