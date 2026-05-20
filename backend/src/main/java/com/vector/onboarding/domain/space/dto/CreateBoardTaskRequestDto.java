package com.vector.onboarding.domain.space.dto;

import com.vector.onboarding.domain.space.BoardTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardTaskRequestDto {
    private String title;
    private BoardTaskStatus status;  // TODO | IN_PROGRESS
    private String assignee;
    private String label;
}
