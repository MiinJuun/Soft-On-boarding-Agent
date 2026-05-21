package com.vector.onboarding.domain.space;

import com.vector.onboarding.domain.space.dto.BoardTaskResponseDto;
import com.vector.onboarding.domain.space.dto.CommitHistoryResponseDto;
import com.vector.onboarding.domain.space.dto.CreateBoardTaskRequestDto;
import com.vector.onboarding.domain.space.dto.CreateSpaceRequestDto;
import com.vector.onboarding.domain.space.dto.CreateSpaceResponseDto;
import com.vector.onboarding.domain.space.dto.JoinSpaceRequestDto;
import com.vector.onboarding.domain.space.dto.UpdateTaskStatusRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    /**
     * 팀 스페이스를 생성합니다.
     * POST /api/spaces
     */
    @PostMapping
    public ResponseEntity<CreateSpaceResponseDto> createSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSpaceRequestDto request) {

        Long userId = Long.valueOf(userDetails.getUsername());
        CreateSpaceResponseDto response = spaceService.createSpace(userId, request);

        // GitHub 커밋 및 파일 목록 비동기 로드 호출
        spaceService.loadGithubCommitsAsync(response.getSpaceId(), request.getRepoUrl());
        response.setMessage("분석 시작됨");

        return ResponseEntity.ok(response);
    }

    /**
     * 팀 코드로 팀 스페이스에 참여합니다.
     * POST /api/spaces/join
     */
    @PostMapping("/join")
    public ResponseEntity<Void> joinSpace(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinSpaceRequestDto request) {

        Long userId = Long.valueOf(userDetails.getUsername());
        spaceService.joinSpace(userId, request.getTeamCode(), request.getJobRole());
        return ResponseEntity.ok().build();
    }

    /**
     * 팀에서 탈퇴합니다.
     * POST /api/spaces/leave
     */
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveSpace(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.valueOf(userDetails.getUsername());
        spaceService.leaveSpace(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 팀 코드의 GitHub 커밋 내역을 가져옵니다.
     * DB에 없으면 자동으로 온디맨드 동기화합니다.
     * GET /api/spaces/{teamCode}/commits
     */
    @GetMapping("/{teamCode}/commits")
    public ResponseEntity<List<CommitHistoryResponseDto>> getCommits(
            @PathVariable String teamCode) {

        List<CommitHistoryResponseDto> response = spaceService.getCommitsByTeamCode(teamCode)
                .stream()
                .map(CommitHistoryResponseDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 팀 코드의 커밋을 GitHub에서 강제 재동기화합니다.
     * 스페이스 전환 시 최신 커밋이 필요할 때 호출합니다.
     * POST /api/spaces/{teamCode}/commits/sync
     */
    @PostMapping("/{teamCode}/commits/sync")
    public ResponseEntity<List<CommitHistoryResponseDto>> syncCommits(
            @PathVariable String teamCode) {

        List<CommitHistoryResponseDto> response = spaceService.syncCommitsByTeamCode(teamCode)
                .stream()
                .map(CommitHistoryResponseDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // =====================================================================
    // BoardTask CRUD  (/api/spaces/{teamCode}/tasks)
    // =====================================================================

    /**
     * 새 태스크를 생성합니다.
     * POST /api/spaces/{teamCode}/tasks
     */
    @PostMapping("/{teamCode}/tasks")
    public ResponseEntity<BoardTaskResponseDto> createTask(
            @PathVariable String teamCode,
            @Valid @RequestBody CreateBoardTaskRequestDto request) {

        BoardTask task = spaceService.createTask(teamCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BoardTaskResponseDto.from(task));
    }

    /**
     * 특정 팀의 모든 태스크를 조회합니다.
     * GET /api/spaces/{teamCode}/tasks
     */
    @GetMapping("/{teamCode}/tasks")
    public ResponseEntity<List<BoardTaskResponseDto>> getTasks(
            @PathVariable String teamCode) {

        List<BoardTaskResponseDto> response = spaceService.getTasksByTeamCode(teamCode)
                .stream()
                .map(BoardTaskResponseDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 태스크를 수정합니다.
     * PUT /api/spaces/tasks/{taskId}
     */
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<BoardTaskResponseDto> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateBoardTaskRequestDto request) {

        BoardTask updated = spaceService.updateTask(taskId, request);
        return ResponseEntity.ok(BoardTaskResponseDto.from(updated));
    }

    /**
     * 태스크 상태만 변경합니다. (체크박스 클릭 → IN_PROGRESS 등)
     * PATCH /api/spaces/{teamCode}/tasks/{taskId}/status
     */
    @PatchMapping("/{teamCode}/tasks/{taskId}/status")
    public ResponseEntity<BoardTaskResponseDto> updateTaskStatus(
            @PathVariable String teamCode,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequestDto request) {

        BoardTask updated = spaceService.updateTaskStatus(taskId, request.getStatus());
        return ResponseEntity.ok(BoardTaskResponseDto.from(updated));
    }

    /**
     * 태스크를 삭제합니다.
     * DELETE /api/spaces/tasks/{taskId}
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        spaceService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
