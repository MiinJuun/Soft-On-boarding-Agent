package com.vector.onboarding.domain.dataview.service;

import com.vector.onboarding.domain.dataview.entity.GithubFileInfo;
import com.vector.onboarding.domain.dataview.entity.SchemaAnalysisResult;
import com.vector.onboarding.domain.dataview.repository.GithubFileRepository;
import com.vector.onboarding.domain.dataview.repository.SchemaAnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataViewService {

    private final GithubFileRepository githubFileRepository;
    private final SchemaAnalysisResultRepository schemaAnalysisResultRepository;
    private final GithubFileFetchService githubFileFetchService;
    private final SchemaParserService schemaParserService;

    @Transactional
    public String getOrAnalyzeSchema(String repositoryUrl) {
        // 1. DB에서 파일 정보 조회
        List<GithubFileInfo> fileInfos = githubFileRepository.findByRepositoryUrl(repositoryUrl);
        String latestCommitHash;
        String combinedFileContents;

        if (fileInfos.isEmpty()) {
            // DB 데이터가 아직 병합되지 않은 상태에서의 임시 테스트용 더미 스키마 파일 주입
            log.info("DB에 파일 정보가 없습니다. 테스트를 위해 임시 스키마(Prisma) 데이터를 만들어 LLM에 전송합니다.");
            latestCommitHash = "dummy-commit-" + System.currentTimeMillis();
            combinedFileContents = "--- File: prisma/schema.prisma ---\n" +
                    "model User {\n" +
                    "  id String @id @default(uuid())\n" +
                    "  email String @unique\n" +
                    "  name String?\n" +
                    "  orders Order[]\n" +
                    "}\n\n" +
                    "model Order {\n" +
                    "  id String @id @default(uuid())\n" +
                    "  userId String\n" +
                    "  totalAmount Decimal\n" +
                    "  status String\n" +
                    "  user User @relation(fields: [userId], references: [id])\n" +
                    "}\n" +
                    "--- File: schema.sql ---\n" +
                    "CREATE TABLE Payment (\n" +
                    "  id UUID PRIMARY KEY,\n" +
                    "  order_id UUID,\n" +
                    "  method VARCHAR,\n" +
                    "  success BOOLEAN\n" +
                    ");";
        } else {
            // 2. 가장 최근 커밋 해시 구하기
            latestCommitHash = fileInfos.get(0).getLastCommitHash(); 

            // 실제로는 파일마다 commit hash를 비교해서 변경된 파일만 가져오도록 최적화 가능
            combinedFileContents = fileInfos.stream()
                    .map(fileInfo -> {
                        String content = githubFileFetchService.fetchFileContent(repositoryUrl, fileInfo.getFilePath());
                        return "--- File: " + fileInfo.getFileName() + " ---\n" + content + "\n";
                    })
                    .collect(Collectors.joining("\n"));
        }

        // 3. 캐시 확인
        Optional<SchemaAnalysisResult> cachedResult = schemaAnalysisResultRepository.findByRepositoryUrl(repositoryUrl);
        
        if (cachedResult.isPresent()) {
            SchemaAnalysisResult result = cachedResult.get();
            if (result.getCommitHash().equals(latestCommitHash)) {
                // 커밋이 동일하면 DB에 저장된 내용 반환 (캐시 히트)
                log.info("캐시 히트: 변경 사항 없음, 기존 분석 데이터를 반환합니다.");
                return result.getAnalyzedJson();
            }
        }

        // 5. 정규식 파서를 통한 분석 (LLM 대체)
        log.info("새로운 커밋 감지됨 (또는 캐시 없음). Java 정규식 기반 스키마 분석을 시작합니다.");
        String analyzedJson = schemaParserService.parseSchema(combinedFileContents);

        // 6. 결과를 DB에 저장/업데이트
        if (cachedResult.isPresent()) {
            SchemaAnalysisResult result = cachedResult.get();
            result.updateAnalysis(latestCommitHash, analyzedJson);
        } else {
            SchemaAnalysisResult newResult = SchemaAnalysisResult.builder()
                    .repositoryUrl(repositoryUrl)
                    .commitHash(latestCommitHash)
                    .analyzedJson(analyzedJson)
                    .build();
            schemaAnalysisResultRepository.save(newResult);
        }

        return analyzedJson;
    }
}
