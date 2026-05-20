package com.vector.onboarding.domain.user;

import com.vector.onboarding.domain.space.Space;
import com.vector.onboarding.domain.space.SpaceMember;
import com.vector.onboarding.domain.space.SpaceMemberRepository;
import com.vector.onboarding.domain.space.SpaceRepository;
import com.vector.onboarding.domain.user.dto.UserProfileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    public UserProfileResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Space space = null;
        String jobRole = null;
        if (user.getTeamCode() != null && !user.getTeamCode().isEmpty()) {
            space = spaceRepository.findByTeamCode(user.getTeamCode()).orElse(null);
            if (space != null) {
                SpaceMember spaceMember = spaceMemberRepository.findBySpaceIdAndUserId(space.getId(), userId).orElse(null);
                if (spaceMember != null) {
                    jobRole = spaceMember.getJobRole();
                }
            }
        }

        return new UserProfileResponseDto(user, space, jobRole);
    }
}
