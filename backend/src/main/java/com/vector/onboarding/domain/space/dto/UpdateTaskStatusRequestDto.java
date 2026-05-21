package com.vector.onboarding.domain.space.dto;

import com.vector.onboarding.domain.space.BoardTaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 태스크 상태만 변경하는 경량 요청 DTO.
 * PATCH /api/spaces/{teamCode}/tasks/{taskId}/status 에서 사용됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskStatusRequestDto {

    @NotNull(message = "status는 필수입니다.")
    private BoardTaskStatus status;
}
