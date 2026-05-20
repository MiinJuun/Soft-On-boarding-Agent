package com.vector.onboarding.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.vector.onboarding.domain.dataview.entity.CommitHistory;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitHistoryResponseDto {
    private String id; // commitSha
    private String title; // message
    private String assignee; // author
    private String commitDate;

    public static CommitHistoryResponseDto from(CommitHistory entity) {
        return CommitHistoryResponseDto.builder()
                .id(entity.getCommitSha())
                .title(entity.getMessage())
                .assignee(entity.getAuthor())
                .commitDate(entity.getCommitDate())
                .build();
    }
}
