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

    /**
     * 팀 격리를 위한 스페이스 ID.
     * 이 필드를 기준으로 팀별 커밋 데이터를 완전히 분리합니다.
     */
    @Column(name = "space_id")
    private Long spaceId;

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
    public CommitHistory(Long spaceId, String repoName, String commitSha, String message, String commitDate, String author) {
        this.spaceId = spaceId;
        this.repoName = repoName;
        this.commitSha = commitSha;
        this.message = message;
        this.commitDate = commitDate;
        this.author = author;
    }
}
