package com.vector.onboarding.domain.dataview.repository;

import com.vector.onboarding.domain.dataview.entity.SchemaAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchemaAnalysisResultRepository extends JpaRepository<SchemaAnalysisResult, Long> {
    Optional<SchemaAnalysisResult> findByRepositoryUrl(String repositoryUrl);
}
