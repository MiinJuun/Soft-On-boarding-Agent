package com.vector.onboarding.domain.dataview.repository;

import com.vector.onboarding.domain.dataview.entity.CommitHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitHistoryRepository extends JpaRepository<CommitHistory, Long> {
    List<CommitHistory> findByRepoName(String repoName);
}
