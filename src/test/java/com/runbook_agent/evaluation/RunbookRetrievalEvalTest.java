package com.runbook_agent.evaluation;

import com.runbook_agent.client.RunbookRetriever;
import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("springai")
class RunbookRetrievalEvalTest {

    @Autowired
    private RunbookRetriever retriever;

    record EvalCase(String query, SymptomType symptomType, String expectedTop1, List<String> expectedTop3) {}

    private static final List<EvalCase> EVAL_CASES = List.of(
        new EvalCase(
            "checkout-service에서 배포 후 500 에러가 증가했습니다",
            SymptomType.HTTP_5XX_INCREASE,
            "RB-001", List.of("RB-001", "RB-003")
        ),
        new EvalCase(
            "order-service에서 DB timeout 로그가 반복됩니다",
            SymptomType.DB_TIMEOUT,
            "RB-004", List.of("RB-004")
        ),
        new EvalCase(
            "payment-service 응답 시간이 3초 이상입니다",
            SymptomType.LATENCY_INCREASE,
            "RB-002", List.of("RB-002")
        ),
        new EvalCase(
            "checkout-service에서 외부 결제 API timeout이 발생했습니다",
            SymptomType.EXTERNAL_API_TIMEOUT,
            "RB-003", List.of("RB-003")
        ),
        new EvalCase(
            "신규 버전 배포 직후 장애가 의심됩니다",
            SymptomType.DEPLOYMENT_RELATED_FAILURE,
            "RB-001", List.of("RB-001")
        ),
        new EvalCase(
            "서버 에러율이 급격히 증가했습니다",
            SymptomType.HTTP_5XX_INCREASE,
            "RB-001", List.of("RB-001", "RB-003")
        ),
        new EvalCase(
            "데이터베이스 연결이 계속 실패합니다",
            SymptomType.DB_TIMEOUT,
            "RB-004", List.of("RB-004")
        ),
        new EvalCase(
            "API 응답 지연이 심각합니다",
            SymptomType.LATENCY_INCREASE,
            "RB-002", List.of("RB-002")
        ),
        new EvalCase(
            "배포 이후 장애가 발생했습니다",
            SymptomType.DEPLOYMENT_RELATED_FAILURE,
            "RB-001", List.of("RB-001")
        ),
        new EvalCase(
            "외부 API 응답이 없습니다",
            SymptomType.EXTERNAL_API_TIMEOUT,
            "RB-003", List.of("RB-003")
        )
    );

    @Test
    void top1Accuracy_shouldBe70PercentOrAbove() {
        long correct = EVAL_CASES.stream()
                .filter(c -> {
                    List<SourceDocument> results = retriever.retrieve(c.query(), c.symptomType());
                    return !results.isEmpty() && results.get(0).runbookId().equals(c.expectedTop1());
                })
                .count();

        double accuracy = (double) correct / EVAL_CASES.size();
        System.out.printf("Top-1 정확도: %d/%d = %.0f%%%n", correct, EVAL_CASES.size(), accuracy * 100);
        assertThat(accuracy).as("Top-1 Runbook 정확도 70%% 이상 기준").isGreaterThanOrEqualTo(0.7);
    }

    @Test
    void top3Accuracy_shouldBe90PercentOrAbove() {
        long correct = EVAL_CASES.stream()
                .filter(c -> {
                    List<SourceDocument> results = retriever.retrieve(c.query(), c.symptomType());
                    List<String> returnedIds = results.stream().map(SourceDocument::runbookId).toList();
                    return c.expectedTop3().stream().allMatch(returnedIds::contains);
                })
                .count();

        double accuracy = (double) correct / EVAL_CASES.size();
        System.out.printf("Top-3 정확도: %d/%d = %.0f%%%n", correct, EVAL_CASES.size(), accuracy * 100);
        assertThat(accuracy).as("Top-3 Runbook 정확도 90%% 이상 기준").isGreaterThanOrEqualTo(0.9);
    }
}
