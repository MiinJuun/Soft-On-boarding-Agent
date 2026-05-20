package com.vector.onboarding.domain.space.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateSpaceRequestDto {

    @NotBlank(message = "팀 스페이스 이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "GitHub 레포지토리 주소는 필수입니다.")
    private String repoUrl;

    @NotBlank(message = "직무(Job Role)는 필수입니다.")
    private String jobRole;

    public CreateSpaceRequestDto(String name, String repoUrl, String jobRole) {
        this.name = name;
        this.repoUrl = repoUrl;
        this.jobRole = jobRole;
    }
}
