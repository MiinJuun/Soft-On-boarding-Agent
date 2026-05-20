package com.vector.onboarding.domain.space;

import com.vector.onboarding.domain.space.dto.CreateSpaceRequestDto;
import com.vector.onboarding.domain.space.dto.CreateSpaceResponseDto;
import com.vector.onboarding.domain.user.User;
import com.vector.onboarding.domain.user.UserRepository;
import com.vector.onboarding.global.exception.SpaceNotFoundException;
import com.vector.onboarding.infrastructure.github.GithubAnalysisService;
import com.vector.onboarding.domain.dataview.repository.GithubFileRepository;
import com.vector.onboarding.domain.dataview.repository.GithubCommitHistoryRepository;
import com.vector.onboarding.domain.dataview.entity.GithubFileInfo;
import com.vector.onboarding.domain.dataview.entity.GithubCommitHistory;
import com.vector.onboarding.domain.dataview.service.GithubFileFetchService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final UserRepository userRepository;
    private final GithubAnalysisService githubAnalysisService;
    private final GithubFileFetchService githubFileFetchService;
    private final GithubFileRepository githubFileRepository;
    private final GithubCommitHistoryRepository githubCommitHistoryRepository;

    private static final int TEAM_CODE_LENGTH = 8;
    private static final String TEAM_CODE_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 팀 스페이스를 생성합니다.
     * 생성자는 자동으로 ADMIN 역할의 SpaceMember로 등록됩니다.
     * GitHub 분석은 사용자 응답을 블로킹하지 않도록 비동기로 실행됩니다.
     */
    public CreateSpaceResponseDto createSpace(Long userId, CreateSpaceRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String teamCode = generateUniqueTeamCode();

        Space space = Space.builder()
                .name(dto.getName())
                .repoUrl(dto.getRepoUrl())
                .teamCode(teamCode)
                .createdBy(userId)
                .build();
        Space savedSpace = spaceRepository.save(space);

        // 생성자를 ADMIN으로 SpaceMember에 등록
        SpaceMember adminMember = SpaceMember.builder()
                .spaceId(savedSpace.getId())
                .userId(userId)
                .memberRole(SpaceMemberRole.ADMIN)
                .build();
        spaceMemberRepository.save(adminMember);

        // User의 소속 teamCode 업데이트
        user.updateTeamCode(teamCode);
        userRepository.save(user);

        // [팀원 연동부] GitHub 레포지토리 분석 - 비동기 실행 (응답 블로킹 없음)
        githubAnalysisService.analyzeAndSaveProjectStructure(savedSpace.getId(), dto.getRepoUrl());
        log.info("Space 생성 완료. spaceId={}, teamCode={}, 분석 비동기 시작", savedSpace.getId(), teamCode);

        return new CreateSpaceResponseDto(savedSpace.getId(), teamCode);
    }

    /**
     * teamCode를 통해 팀 스페이스에 참여합니다.
     * 존재하지 않는 코드일 경우 {@link SpaceNotFoundException}을 던집니다.
     */
    public void joinSpace(Long userId, String teamCode) {
        Space space = spaceRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new SpaceNotFoundException(teamCode));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (spaceMemberRepository.existsBySpaceIdAndUserId(space.getId(), userId)) {
            throw new IllegalArgumentException("이미 참여한 팀 스페이스입니다.");
        }

        // MEMBER 역할로 SpaceMember 등록
        SpaceMember member = SpaceMember.builder()
                .spaceId(space.getId())
                .userId(userId)
                .memberRole(SpaceMemberRole.MEMBER)
                .build();
        spaceMemberRepository.save(member);

        // User의 소속 teamCode 업데이트
        user.updateTeamCode(teamCode);
        userRepository.save(user);

        log.info("Space 참여 완료. userId={}, spaceId={}, teamCode={}", userId, space.getId(), teamCode);
    }

    /**
     * DB에 존재하지 않는 고유한 8자리 팀 코드를 생성합니다.
     * 패키지 수준 접근자(package-private)로 선언하여 단위 테스트에서 직접 검증 가능하게 합니다.
     */
    String generateUniqueTeamCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (spaceRepository.existsByTeamCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(TEAM_CODE_LENGTH);
        for (int i = 0; i < TEAM_CODE_LENGTH; i++) {
            sb.append(TEAM_CODE_CHARS.charAt(RANDOM.nextInt(TEAM_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * [비동기 로직] GitHub Git Trees API 및 Commits API를 호출하여 데이터를 로드합니다.
     * 프론트엔드 응답을 블로킹하지 않고 백그라운드에서 실행됩니다.
     * 
     * @param spaceId 스페이스 ID
     * @param repoUrl GitHub 레포지토리 URL
     */
    @Async
    public void loadGithubCommitsAsync(Long spaceId, String repoUrl) {
        log.info("비동기 데이터 로드 시작 - SpaceID: {}, Repo: {}", spaceId, repoUrl);
        try {
            // URL 파싱 (예: https://github.com/vector/onboarding -> owner: vector, repo: onboarding)
            String urlPath = repoUrl.replace("https://github.com/", "").replace(".git", "");
            String[] parts = urlPath.split("/");
            if (parts.length < 2) {
                log.error("잘못된 레포지토리 URL 형식입니다: {}", repoUrl);
                return;
            }
            String owner = parts[0];
            String repo = parts[1];

            // 1. 로직 B: GitHub Commits API 호출 (최근 100개 커밋 내역과 Diff 매핑 후 저장)
            JsonNode commits = githubFileFetchService.fetchCommits(owner, repo);
            String latestCommitSha = "main"; // 기본값
            
            if (commits != null && commits.isArray() && commits.size() > 0) {
                latestCommitSha = commits.get(0).get("sha").asText();
                List<GithubCommitHistory> commitHistories = new ArrayList<>();
                
                for (JsonNode commitNode : commits) {
                    String sha = commitNode.get("sha").asText();
                    String message = commitNode.get("commit").get("message").asText();
                    String author = commitNode.get("commit").get("author").get("name").asText();
                    String dateStr = commitNode.get("commit").get("author").get("date").asText();
                    LocalDateTime committedAt = ZonedDateTime.parse(dateStr).toLocalDateTime();
                    
                    // Diff 가져오기
                    String diff = githubFileFetchService.fetchCommitDiff(owner, repo, sha);
                    
                    GithubCommitHistory history = GithubCommitHistory.builder()
                            .repositoryUrl(repoUrl)
                            .commitHash(sha)
                            .commitMessage(message)
                            .authorName(author)
                            .diffContent(diff)
                            .committedAt(committedAt)
                            .build();
                            
                    commitHistories.add(history);
                }
                // 일괄 저장
                githubCommitHistoryRepository.saveAll(commitHistories);
                log.info("최근 100개의 커밋 내역 및 Diff 저장 완료");
            }

            // 2. 로직 A: GitHub Git Trees API 호출 및 파일 경로 목록 저장
            JsonNode treeResponse = githubFileFetchService.fetchGitTree(owner, repo, latestCommitSha);
            if (treeResponse != null && treeResponse.has("tree")) {
                JsonNode treeArray = treeResponse.get("tree");
                List<GithubFileInfo> fileInfos = new ArrayList<>();
                
                for (JsonNode node : treeArray) {
                    if ("blob".equals(node.get("type").asText())) {
                        String path = node.get("path").asText();
                        String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                        
                        GithubFileInfo fileInfo = GithubFileInfo.builder()
                                .repositoryUrl(repoUrl)
                                .filePath(path)
                                .fileName(fileName)
                                .lastCommitHash(latestCommitSha)
                                .lastSyncedAt(LocalDateTime.now())
                                .build();
                                
                        fileInfos.add(fileInfo);
                    }
                }
                // 일괄 저장
                githubFileRepository.saveAll(fileInfos);
                log.info("파일 경로 목록 저장 완료. 총 {}개 파일", fileInfos.size());
            }

            // TODO: LLM 분류 로직 뼈대 (SchemaAnalysisResult 활용)
            // SchemaAnalysisResult result = SchemaAnalysisResult.builder()...
            // schemaAnalysisResultRepository.save(result);
            
            log.info("비동기 데이터 로드 완료 - SpaceID: {}", spaceId);

        } catch (Exception e) {
            log.error("비동기 데이터 로드 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
