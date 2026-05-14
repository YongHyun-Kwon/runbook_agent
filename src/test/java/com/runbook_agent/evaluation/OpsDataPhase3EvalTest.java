package com.runbook_agent.evaluation;

import com.runbook_agent.dto.IncidentAnalyzeRequest;
import com.runbook_agent.dto.IncidentAnalyzeResponse;
import com.runbook_agent.service.IncidentAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OpsDataPhase3EvalTest {

    @Autowired
    private IncidentAnalysisService analysisService;

    // 완료 기준 1: 응답에 운영 데이터 Evidence 포함
    @Test
    void analyze_knownService_includesOpsDataEvidence() {
        var request = new IncidentAnalyzeRequest(
            "checkout-service",
            "배포 후 500 에러가 증가했습니다",
            "LAST_30_MINUTES"
        );

        IncidentAnalyzeResponse response = analysisService.analyze(request);

        // OPS 데이터가 evidence에 포함되어야 함
        boolean hasOpsEvidence = response.evidence().stream()
            .anyMatch(e -> "ACTION_RESULT".equals(e.type()));
        assertThat(hasOpsEvidence).isTrue();

        // Runbook evidence도 함께 포함되어야 함
        boolean hasRunbookEvidence = response.evidence().stream()
            .anyMatch(e -> "RUNBOOK".equals(e.type()));
        assertThat(hasRunbookEvidence).isTrue();

        // 성공한 action이 존재해야 함
        boolean hasSuccessAction = response.calledActions().stream()
            .anyMatch(a -> "SUCCESS".equals(a.status()));
        assertThat(hasSuccessAction).isTrue();
    }

    // 완료 기준 2: Action 실패 상황에서도 유효한 응답 반환
    @Test
    void analyze_unknownService_returnsValidResponseWithFailedActions() {
        var request = new IncidentAnalyzeRequest(
            "unknown-service",
            "500 에러가 발생했습니다",
            null
        );

        IncidentAnalyzeResponse response = analysisService.analyze(request);

        // 응답 자체는 유효해야 함
        assertThat(response.requestId()).isNotBlank();
        assertThat(response.summary()).isNotNull();

        // 모든 action이 FAILED 상태
        boolean allFailed = response.calledActions().stream()
            .allMatch(a -> "FAILED".equals(a.status()));
        assertThat(allFailed).isTrue();

        // ops evidence 없어도 Runbook evidence는 존재할 수 있음
        assertThat(response.evidence()).isNotNull();

        // 위험 조치는 포함하지 않음
        boolean noRiskyStep = response.recommendedSteps().stream()
            .noneMatch(s -> s.step().contains("rollback 실행") || s.step().contains("재시작"));
        assertThat(noRiskyStep).isTrue();
    }

    // edge case: 배포는 있었지만 3시간 전 — MEDIUM 이하 confidence 유지
    @Test
    void analyze_paymentService_doesNotOverconfidentlyBlameDeploy() {
        var request = new IncidentAnalyzeRequest(
            "payment-service",
            "배포 후 오류가 발생했습니다",
            null
        );

        IncidentAnalyzeResponse response = analysisService.analyze(request);

        // 확신도가 HIGH가 아니어야 함 (3시간 전 배포이므로 단정 금지)
        boolean noHighConfidence = response.rootCauseCandidates().stream()
            .noneMatch(c -> "HIGH".equals(c.confidence().name()));
        assertThat(noHighConfidence).isTrue();
    }
}
