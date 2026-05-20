package com.vector.onboarding.domain.space;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardTaskRepository extends JpaRepository<BoardTask, Long> {
    List<BoardTask> findBySpaceId(Long spaceId);
    List<BoardTask> findBySpaceIdAndStatus(Long spaceId, BoardTaskStatus status);
}
