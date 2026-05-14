package com.runbook_agent.client;

import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// bedrock 프로파일이 아닐 때 기본으로 사용 (Phase 1~4 테스트 보호)
@Component
@Profile("!bedrock")
public class MockRunbookRetriever implements RunbookRetriever {

    private static final Map<SymptomType, List<SourceDocument>> RUNBOOK_MAP = Map.of(
        SymptomType.HTTP_5XX_INCREASE, List.of(
            new SourceDocument("RB-001", "배포 직후 5xx 증가 대응 절차", "RUNBOOK"),
            new SourceDocument("RB-003", "외부 API timeout 대응 절차", "RUNBOOK")
        ),
        SymptomType.LATENCY_INCREASE, List.of(
            new SourceDocument("RB-002", "응답 지연 대응 절차", "RUNBOOK")
        ),
        SymptomType.DB_TIMEOUT, List.of(
            new SourceDocument("RB-004", "DB timeout 대응 절차", "RUNBOOK")
        ),
        SymptomType.EXTERNAL_API_TIMEOUT, List.of(
            new SourceDocument("RB-003", "외부 API timeout 대응 절차", "RUNBOOK")
        ),
        SymptomType.DEPLOYMENT_RELATED_FAILURE, List.of(
            new SourceDocument("RB-001", "배포 직후 5xx 증가 대응 절차", "RUNBOOK")
        )
    );

    @Override
    public List<SourceDocument> retrieve(String query, SymptomType symptomType) {
        return RUNBOOK_MAP.getOrDefault(symptomType, List.of());
    }
}
