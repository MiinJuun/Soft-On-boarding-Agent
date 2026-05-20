package com.vector.onboarding.domain.dataview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CommitHistory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", columnDefinition = "TEXT")
    private String repoName;

    @Column(name = "commit_sha", columnDefinition = "TEXT")
    private String commitSha;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "commit_date", columnDefinition = "TEXT")
    private String commitDate;

    @Column(columnDefinition = "TEXT")
    private String author;

    @Builder
    public CommitHistory(String repoName, String commitSha, String message, String commitDate, String author) {
        this.repoName = repoName;
        this.commitSha = commitSha;
        this.message = message;
        this.commitDate = commitDate;
        this.author = author;
    }
}
