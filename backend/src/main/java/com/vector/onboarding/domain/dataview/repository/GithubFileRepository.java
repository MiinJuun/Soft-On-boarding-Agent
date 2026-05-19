package com.vector.onboarding.domain.dataview.repository;

import com.vector.onboarding.domain.dataview.entity.GithubFileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GithubFileRepository extends JpaRepository<GithubFileInfo, Long> {
    List<GithubFileInfo> findByRepositoryUrl(String repositoryUrl);
}
