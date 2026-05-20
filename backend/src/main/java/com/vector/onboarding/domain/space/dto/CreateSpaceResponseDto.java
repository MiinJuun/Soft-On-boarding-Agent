package com.vector.onboarding.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateSpaceResponseDto {

    private Long spaceId;
    private String teamCode;
    private String message;

    public CreateSpaceResponseDto(Long spaceId, String teamCode) {
        this.spaceId = spaceId;
        this.teamCode = teamCode;
    }
}
