package com.vector.onboarding.domain.dataview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GithubFileFetchService {

    @Value("${app.github.api-url}")
    private String githubApiUrl;

    @Value("${app.github.system-token}")
    private String systemToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchFileContent(String repositoryUrl, String filePath) {
        log.info("GitHub API 호출하여 파일 내용을 가져옵니다. repository: {}, path: {}", repositoryUrl, filePath);
        
        // Github API URL 생성 (예: https://api.github.com/repos/{owner}/{repo}/contents/{path})
        // repositoryUrl 포맷 파싱 등의 상세 로직은 실 구현 시 추가
        String url = githubApiUrl + "/repos/" + repositoryUrl + "/contents/" + filePath;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + systemToken);
            headers.set("Accept", "application/vnd.github.v3.raw");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("GitHub 파일 가져오기 실패: {}", e.getMessage());
            return "// 파일 내용을 가져오는데 실패했습니다: " + filePath;
        }
    }
}
