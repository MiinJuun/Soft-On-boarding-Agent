package com.vector.onboarding.global.security;

import com.vector.onboarding.domain.user.Role;
import com.vector.onboarding.domain.user.User;
import com.vector.onboarding.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String accessToken = userRequest.getAccessToken().getTokenValue();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String profileImageUrl = (String) attributes.get("avatar_url");

        User user = userRepository.findByGithubId(githubId)
                .map(existingUser -> existingUser.update(username, email, profileImageUrl, accessToken))
                .orElse(User.builder()
                        .githubId(githubId)
                        .username(username)
                        .email(email)
                        .profileImageUrl(profileImageUrl)
                        .githubAccessToken(accessToken)
                        .role(Role.USER)
                        .build());

        userRepository.save(user);

        return new CustomUserDetails(user, attributes);
    }
}
