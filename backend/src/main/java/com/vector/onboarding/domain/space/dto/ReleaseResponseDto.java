package com.vector.onboarding.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseResponseDto {
    private Long id;
    private String name;
    private String date;
    private String type; // "release" | "hotfix"
    private Integer completedTasks;
    private Integer totalTasks;
}
