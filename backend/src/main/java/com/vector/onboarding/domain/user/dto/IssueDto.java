package com.vector.onboarding.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueDto {
    private String id;
    private Long releaseId;
    private String title;
    private String status; // 'Todo', 'In Progress', 'Done'
    private String role;   // 'Frontend', 'Backend', 'DevOps', 'Design'
    private String assignee;
    private String avatarUrl;
}