package com.runbook_agent.service;

import com.runbook_agent.domain.Severity;
import com.runbook_agent.domain.SymptomType;
import org.springframework.stereotype.Component;

@Component
public class IncidentInputParser {

    public SymptomType classifySymptom(String message) {
        String lower = message.toLowerCase();
        if (isDbTimeout(lower))                return SymptomType.DB_TIMEOUT;
        if (isExternalApiTimeout(lower))       return SymptomType.EXTERNAL_API_TIMEOUT;
        if (isHttp5xxIncrease(lower))          return SymptomType.HTTP_5XX_INCREASE;
        if (isDeploymentRelatedFailure(lower)) return SymptomType.DEPLOYMENT_RELATED_FAILURE;
        if (isLatencyIncrease(lower))          return SymptomType.LATENCY_INCREASE;
        return SymptomType.UNKNOWN;
    }

    public Severity assessSeverity(SymptomType symptomType) {
        return switch (symptomType) {
            case HTTP_5XX_INCREASE, DB_TIMEOUT, DEPLOYMENT_RELATED_FAILURE -> Severity.HIGH;
            case LATENCY_INCREASE, EXTERNAL_API_TIMEOUT -> Severity.MEDIUM;
            default -> Severity.UNKNOWN;
        };
    }

    // DB 관련 키워드 + 연결/장애 키워드 조합
    private boolean isDbTimeout(String lower) {
        return containsAny(lower, "db", "database", "데이터베이스")
            && containsAny(lower, "timeout", "타임아웃", "연결", "실패");
    }

    // 외부 키워드 + API/timeout 조합
    private boolean isExternalApiTimeout(String lower) {
        return containsAny(lower, "외부", "external")
            && containsAny(lower, "api", "timeout", "타임아웃", "응답 없");
    }

    // 숫자 기반 HTTP 에러 표현 → 가장 명확한 지표
    private boolean isHttp5xxIncrease(String lower) {
        return containsAny(lower, "500", "5xx", "서버 에러", "에러율", "http 500");
    }

    // 배포 키워드 + 문제 키워드 조합
    private boolean isDeploymentRelatedFailure(String lower) {
        return containsAny(lower, "배포", "deploy", "릴리즈", "release", "버전")
            && containsAny(lower, "장애", "에러", "오류", "문제", "실패");
    }

    // 응답 시간 관련 표현
    private boolean isLatencyIncrease(String lower) {
        return containsAny(lower, "지연", "latency", "응답 시간", "느림", "느리", "느립", "slow");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
