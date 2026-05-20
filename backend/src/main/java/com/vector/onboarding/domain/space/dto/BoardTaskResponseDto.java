package com.vector.onboarding.domain.space.dto;

import com.vector.onboarding.domain.space.BoardTask;
import com.vector.onboarding.domain.space.BoardTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardTaskResponseDto {
    private Long id;
    private Long spaceId;
    private String title;
    private BoardTaskStatus status;
    private String assignee;
    private String label;
    private LocalDateTime createdAt;

    public static BoardTaskResponseDto from(BoardTask entity) {
        return BoardTaskResponseDto.builder()
                .id(entity.getId())
                .spaceId(entity.getSpaceId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .assignee(entity.getAssignee())
                .label(entity.getLabel())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
