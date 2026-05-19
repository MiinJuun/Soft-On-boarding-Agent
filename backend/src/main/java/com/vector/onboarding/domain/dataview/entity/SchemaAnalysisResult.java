package com.vector.onboarding.domain.dataview.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "schema_analysis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchemaAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repositoryUrl;

    @Column(nullable = false)
    private String commitHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String analyzedJson;

    private LocalDateTime analyzedAt;

    @Builder
    public SchemaAnalysisResult(String repositoryUrl, String commitHash, String analyzedJson) {
        this.repositoryUrl = repositoryUrl;
        this.commitHash = commitHash;
        this.analyzedJson = analyzedJson;
        this.analyzedAt = LocalDateTime.now();
    }

    public void updateAnalysis(String commitHash, String analyzedJson) {
        this.commitHash = commitHash;
        this.analyzedJson = analyzedJson;
        this.analyzedAt = LocalDateTime.now();
    }
}
