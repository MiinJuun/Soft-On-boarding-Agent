package com.vector.onboarding.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponseDto {
    private String id;
    private Long releaseId;
    private String title;
    private String status; // "Todo" | "In Progress" | "Done"
    private String role; // "Frontend" | "Backend" | "DevOps" | "Design"
    private String assignee;
    private String avatarUrl;
}
