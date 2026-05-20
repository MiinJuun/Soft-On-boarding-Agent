package com.vector.onboarding.domain.dataview.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class GithubFileFetchService {

    private final WebClient webClient;

    public GithubFileFetchService(
            WebClient.Builder webClientBuilder,
            @Value("${app.github.api-url:https://api.github.com}") String githubApiUrl,
            @Value("${app.github.system-token:}") String systemToken) {
        
        WebClient.Builder builder = webClientBuilder.baseUrl(githubApiUrl);
        
        if (systemToken != null && !systemToken.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + systemToken);
        }
        
        this.webClient = builder
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    /**
     * GitHub Git Trees API 호출 (재귀적)
     */
    public JsonNode fetchGitTree(String owner, String repo, String branchSha) {
        log.info("Fetching git tree for {}/{} at {}", owner, repo, branchSha);
        return webClient.get()
                .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branchSha)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * GitHub Commits API 호출 (최근 100개)
     */
    public JsonNode fetchCommits(String owner, String repo) {
        log.info("Fetching commits for {}/{}", owner, repo);
        return webClient.get()
                .uri("/repos/{owner}/{repo}/commits?per_page=100", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * 개별 커밋 Diff 호출
     */
    public String fetchCommitDiff(String owner, String repo, String commitSha) {
        log.info("Fetching diff for {}/{} commit {}", owner, repo, commitSha);
        try {
            return webClient.get()
                    .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, commitSha)
                    .header("Accept", "application/vnd.github.v3.diff")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch diff for commit {}: {}", commitSha, e.getMessage());
            return "";
        }
    }

    /**
     * GitHub API 호출하여 파일 내용을 가져옵니다. (DataViewService 연동용)
     */
    public String fetchFileContent(String repositoryUrl, String filePath) {
        log.info("Fetching file content for repo: {}, path: {}", repositoryUrl, filePath);
        
        String urlPath = repositoryUrl.replace("https://github.com/", "").replace(".git", "");
        String[] parts = urlPath.split("/");
        if (parts.length < 2) {
            log.error("잘못된 레포지토리 URL 형식입니다: {}", repositoryUrl);
            return "// 파일 내용을 가져오는데 실패했습니다: " + filePath;
        }
        String owner = parts[0];
        String repo = parts[1];

        try {
            String uri = "/repos/" + owner + "/" + repo + "/contents/" + filePath;
            return webClient.get()
                    .uri(uri)
                    .header("Accept", "application/vnd.github.v3.raw")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("GitHub 파일 가져오기 실패: {}", e.getMessage());
            return "// 파일 내용을 가져오는데 실패했습니다: " + filePath;
        }
    }
}
