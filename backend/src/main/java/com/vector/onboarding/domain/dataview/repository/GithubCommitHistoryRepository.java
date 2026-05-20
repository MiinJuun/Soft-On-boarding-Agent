package com.vector.onboarding.domain.dataview.repository;

import com.vector.onboarding.domain.dataview.entity.GithubCommitHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubCommitHistoryRepository extends JpaRepository<GithubCommitHistory, Long> {
}
