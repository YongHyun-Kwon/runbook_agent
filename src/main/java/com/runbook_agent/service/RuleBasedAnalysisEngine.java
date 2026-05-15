package com.runbook_agent.service;

import com.runbook_agent.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!springai")
public class RuleBasedAnalysisEngine implements AnalysisEngine {

    @Override
    public AnalysisResult analyze(AnalysisContext context) {
        List<RootCauseCandidate> candidates = buildCandidates(context);
        List<RecommendedStep> steps = buildRecommendedSteps(context.symptomType());
        return new AnalysisResult(candidates, steps);
    }

    private List<RootCauseCandidate> buildCandidates(AnalysisContext context) {
        if (context.allEvidence().isEmpty()) return List.of();
        List<String> refs = context.allEvidence().stream().map(Evidence::id).toList();
        Confidence confidence = context.opsEvidence().isEmpty() ? Confidence.LOW : Confidence.MEDIUM;
        return List.of(new RootCauseCandidate(
                getCandidateName(context.symptomType()),
                confidence,
                refs,
                buildUncertainty(context.symptomType(), context.opsEvidence())
        ));
    }

    private String getCandidateName(SymptomType symptomType) {
        return switch (symptomType) {
            case HTTP_5XX_INCREASE -> "서버 내부 오류 또는 최근 배포 영향";
            case LATENCY_INCREASE -> "외부 의존성 지연 또는 리소스 부족";
            case DB_TIMEOUT -> "DB connection pool 고갈 또는 슬로우 쿼리";
            case EXTERNAL_API_TIMEOUT -> "외부 API 장애 또는 네트워크 이슈";
            case DEPLOYMENT_RELATED_FAILURE -> "최근 배포 변경사항 영향";
            default -> "원인 미확정";
        };
    }

    private String buildUncertainty(SymptomType symptomType, List<Evidence> opsEvidence) {
        if (opsEvidence.isEmpty()) {
            return "운영 데이터 조회에 실패했습니다. 수동으로 운영 데이터를 확인하세요.";
        }
        return switch (symptomType) {
            case HTTP_5XX_INCREASE -> "배포 시점과 에러 증가 시점 간격, exception message 패턴 확인이 필요합니다.";
            case LATENCY_INCREASE -> "외부 의존성 응답 시간과 DB connection pool 상태 추가 확인이 필요합니다.";
            case DB_TIMEOUT -> "슬로우 쿼리 로그와 connection pool 최대치 설정 확인이 필요합니다.";
            case EXTERNAL_API_TIMEOUT -> "외부 API 공식 상태 페이지 확인 후 판단하세요.";
            case DEPLOYMENT_RELATED_FAILURE -> "배포 변경사항과 에러 발생 패턴의 인과관계 확인이 필요합니다.";
            default -> "추가 정보 없이는 원인을 특정할 수 없습니다.";
        };
    }

    private List<RecommendedStep> buildRecommendedSteps(SymptomType symptomType) {
        return switch (symptomType) {
            case HTTP_5XX_INCREASE -> List.of(
                    new RecommendedStep(1, "최근 배포 버전의 변경사항을 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(2, "CloudWatch에서 공통 exception message를 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(3, "rollback은 담당자 승인 후 검토하세요.", StepType.APPROVAL_REQUIRED, true)
            );
            case LATENCY_INCREASE -> List.of(
                    new RecommendedStep(1, "서비스 health check 상태를 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(2, "외부 의존 서비스 응답 시간을 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(3, "DB connection pool 상태를 확인하세요.", StepType.CHECK_ONLY, false)
            );
            case DB_TIMEOUT -> List.of(
                    new RecommendedStep(1, "DB connection pool 현황을 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(2, "슬로우 쿼리 로그를 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(3, "DB 수정은 담당자 승인 후 진행하세요.", StepType.APPROVAL_REQUIRED, true)
            );
            case EXTERNAL_API_TIMEOUT -> List.of(
                    new RecommendedStep(1, "외부 API 상태 페이지를 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(2, "timeout 설정값을 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(3, "circuit breaker 동작 여부를 확인하세요.", StepType.CHECK_ONLY, false)
            );
            case DEPLOYMENT_RELATED_FAILURE -> List.of(
                    new RecommendedStep(1, "최근 배포 내역과 변경사항을 확인하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(2, "배포 전후 에러율 변화를 비교하세요.", StepType.CHECK_ONLY, false),
                    new RecommendedStep(3, "rollback은 담당자 승인 후 검토하세요.", StepType.APPROVAL_REQUIRED, true)
            );
            default -> List.of(
                    new RecommendedStep(1, "서비스 상태를 확인하세요.", StepType.CHECK_ONLY, false)
            );
        };
    }
}
