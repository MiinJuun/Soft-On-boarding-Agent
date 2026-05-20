package com.vector.onboarding.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseDto {
    private Long id;
    private String name;
    private String date;
    private String type; // 'release' or 'hotfix'
    private int completedTasks;
    private int totalTasks;
}