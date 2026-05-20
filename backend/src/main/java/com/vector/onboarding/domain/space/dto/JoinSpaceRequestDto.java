package com.vector.onboarding.domain.space.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JoinSpaceRequestDto {

    @NotBlank(message = "팀 코드는 필수입니다.")
    private String teamCode;

    @NotBlank(message = "직무(Job Role)는 필수입니다.")
    private String jobRole;

    public JoinSpaceRequestDto(String teamCode, String jobRole) {
        this.teamCode = teamCode;
        this.jobRole = jobRole;
    }
}
