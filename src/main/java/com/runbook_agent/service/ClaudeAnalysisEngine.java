package com.runbook_agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runbook_agent.client.RunbookDocumentStore;
import com.runbook_agent.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("springai")
public class ClaudeAnalysisEngine implements AnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAnalysisEngine.class);

    // 섹션 12.2 Agent Instruction — Claude API System Prompt
    private static final String SYSTEM_PROMPT = """
            당신은 엔터프라이즈 백엔드 서비스의 장애 대응을 돕는 Runbook Agent입니다.

            목표:
            - 사용자가 입력한 장애 상황을 분석합니다.
            - 관련 Runbook과 운영 데이터 근거를 바탕으로 원인 후보와 1차 확인 절차를 제안합니다.

            제약:
            - rollback, restart, scale-out, DB 수정, 배포 같은 쓰기 작업은 절대 수행하지 않습니다.
            - 근거가 부족하면 원인을 확정하지 않습니다.
            - Runbook 또는 운영 데이터에 없는 내용을 사실처럼 말하지 않습니다.
            - 민감정보, 인증정보, 토큰, 비밀번호는 응답하지 않습니다.
            - 위험 작업은 담당자 승인 필요성으로만 안내합니다.
            - 반드시 JSON 형식으로만 응답합니다. 다른 텍스트는 포함하지 않습니다.
            """;

    private final ChatClient chatClient;
    private final RunbookDocumentStore runbookDocumentStore;
    private final ObjectMapper objectMapper;

    public ClaudeAnalysisEngine(ChatClient.Builder chatClientBuilder,
                                RunbookDocumentStore runbookDocumentStore,
                                ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.runbookDocumentStore = runbookDocumentStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalysisResult analyze(AnalysisContext context) {
        String prompt = buildUserPrompt(context);
        try {
            String response = chatClient.prompt().user(prompt).call().content();
            return parseResponse(response, context.allEvidence());
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            return fallbackResult();
        }
    }

    // RAG 흐름: 검색된 Runbook 내용 + Ops 데이터를 Claude 입력(컨텍스트)으로 조합
    private String buildUserPrompt(AnalysisContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 장애 상황을 분석하고 JSON으로만 응답하세요.\n\n");
        sb.append("서비스명: ").append(context.serviceName()).append("\n");
        sb.append("증상 유형: ").append(context.symptomType()).append("\n");
        sb.append("증상 설명: ").append(context.message()).append("\n\n");

        sb.append("=== 관련 Runbook ===\n");
        if (context.runbooks().isEmpty()) {
            sb.append("(관련 Runbook 없음)\n");
        } else {
            for (var doc : context.runbooks()) {
                sb.append("[").append(doc.runbookId()).append("] ").append(doc.title()).append("\n");
                runbookDocumentStore.getExcerpt(doc.runbookId()).ifPresent(excerpt ->
                        sb.append(excerpt).append("\n")
                );
                sb.append("\n");
            }
        }

        sb.append("=== 운영 데이터 ===\n");
        if (context.opsEvidence().isEmpty()) {
            sb.append("(운영 데이터 조회 실패 또는 없음)\n");
        } else {
            for (var e : context.opsEvidence()) {
                sb.append("[").append(e.sourceId()).append("] ").append(e.content()).append("\n");
            }
        }

        sb.append("""

                === 응답 형식 ===
                다음 JSON 형식으로만 응답하세요. confidence는 LOW/MEDIUM/HIGH 중 하나, type은 CHECK_ONLY/APPROVAL_REQUIRED 중 하나입니다:
                {
                  "rootCauseCandidates": [
                    {
                      "name": "원인 후보 이름",
                      "confidence": "MEDIUM",
                      "uncertainty": "추가 확인이 필요한 내용"
                    }
                  ],
                  "recommendedSteps": [
                    {
                      "order": 1,
                      "step": "확인 절차 내용",
                      "type": "CHECK_ONLY",
                      "requiresApproval": false
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    private AnalysisResult parseResponse(String response, List<Evidence> allEvidence) {
        String json = stripMarkdownCodeBlock(response);
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> evidenceRefs = allEvidence.stream().map(Evidence::id).toList();

            List<RootCauseCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root.path("rootCauseCandidates")) {
                candidates.add(new RootCauseCandidate(
                        node.path("name").asText("원인 미확정"),
                        parseConfidence(node.path("confidence").asText()),
                        evidenceRefs,
                        node.path("uncertainty").asText("")
                ));
            }

            List<RecommendedStep> steps = new ArrayList<>();
            for (JsonNode node : root.path("recommendedSteps")) {
                steps.add(new RecommendedStep(
                        node.path("order").asInt(steps.size() + 1),
                        node.path("step").asText(),
                        parseStepType(node.path("type").asText()),
                        node.path("requiresApproval").asBoolean(false)
                ));
            }

            return new AnalysisResult(candidates, steps);
        } catch (Exception e) {
            log.warn("Claude 응답 파싱 실패: {}", e.getMessage());
            return fallbackResult();
        }
    }

    private String stripMarkdownCodeBlock(String response) {
        String trimmed = response.strip();
        if (!trimmed.startsWith("```")) return trimmed;
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return (start >= 0 && end > start) ? trimmed.substring(start, end + 1) : trimmed;
    }

    private Confidence parseConfidence(String value) {
        try {
            return Confidence.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Confidence.MEDIUM;
        }
    }

    private StepType parseStepType(String value) {
        try {
            return StepType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return StepType.CHECK_ONLY;
        }
    }

    private AnalysisResult fallbackResult() {
        return new AnalysisResult(
                List.of(new RootCauseCandidate("원인 분석 실패", Confidence.LOW, List.of(),
                        "Claude API 응답을 처리할 수 없습니다. 수동으로 확인하세요.")),
                List.of(new RecommendedStep(1, "서비스 상태를 수동으로 확인하세요.", StepType.CHECK_ONLY, false))
        );
    }
}
