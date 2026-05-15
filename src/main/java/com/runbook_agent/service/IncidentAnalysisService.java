package com.runbook_agent.service;

import com.runbook_agent.common.RequestIdGenerator;
import com.runbook_agent.domain.*;
import com.runbook_agent.dto.IncidentAnalyzeRequest;
import com.runbook_agent.dto.IncidentAnalyzeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncidentAnalysisService {

    private final RequestIdGenerator requestIdGenerator;
    private final IncidentInputParser inputParser;
    private final RunbookSearchService runbookSearchService;
    private final ActionDecider actionDecider;
    private final OpsDataService opsDataService;
    private final AnalysisEngine analysisEngine;

    public IncidentAnalysisService(RequestIdGenerator requestIdGenerator,
                                   IncidentInputParser inputParser,
                                   RunbookSearchService runbookSearchService,
                                   ActionDecider actionDecider,
                                   OpsDataService opsDataService,
                                   AnalysisEngine analysisEngine) {
        this.requestIdGenerator = requestIdGenerator;
        this.inputParser = inputParser;
        this.runbookSearchService = runbookSearchService;
        this.actionDecider = actionDecider;
        this.opsDataService = opsDataService;
        this.analysisEngine = analysisEngine;
    }

    public IncidentAnalyzeResponse analyze(IncidentAnalyzeRequest request) {
        String requestId = requestIdGenerator.generate();

        SymptomType symptomType = inputParser.classifySymptom(request.message());
        Severity severity = inputParser.assessSeverity(symptomType);

        List<SourceDocument> runbooks = runbookSearchService.search(request.message(), symptomType);
        List<String> actionNames = actionDecider.decide(symptomType, request.message());

        OpsDataResult opsResult = opsDataService.fetch(request.serviceName(), actionNames, symptomType);

        List<Evidence> allEvidence = new ArrayList<>(buildRunbookEvidence(runbooks));
        allEvidence.addAll(opsResult.evidence());

        // AnalysisEngine: 기본 프로파일 = RuleBasedAnalysisEngine, springai 프로파일 = ClaudeAnalysisEngine
        AnalysisContext context = new AnalysisContext(
                request.serviceName(), request.message(), symptomType,
                runbooks, allEvidence, opsResult.evidence()
        );
        AnalysisResult analysis = analysisEngine.analyze(context);

        String timeRange = request.timeRange() != null ? request.timeRange() : "LAST_30_MINUTES";
        var summary = new IncidentSummary(request.serviceName(), symptomType, severity, timeRange);

        return new IncidentAnalyzeResponse(
                requestId, summary, opsResult.calledActions(), allEvidence,
                analysis.candidates(), analysis.steps(), runbooks,
                List.of("이 Agent는 read-only 모드이며 rollback을 직접 수행하지 않습니다.")
        );
    }

    private List<Evidence> buildRunbookEvidence(List<SourceDocument> runbooks) {
        List<Evidence> evidence = new ArrayList<>();
        for (int i = 0; i < runbooks.size(); i++) {
            var doc = runbooks.get(i);
            evidence.add(new Evidence(
                    "RB-EVIDENCE-" + String.format("%03d", i + 1),
                    "RUNBOOK",
                    doc.runbookId(),
                    getRunbookSummary(doc.runbookId())
            ));
        }
        return evidence;
    }

    private String getRunbookSummary(String runbookId) {
        return switch (runbookId) {
            case "RB-001" -> "배포 이후 30분 이내 5xx 증가 시 최근 배포 버전을 확인한다.";
            case "RB-002" -> "응답 지연 발생 시 서비스 health check와 에러율을 함께 확인한다.";
            case "RB-003" -> "외부 API timeout 발생 시 외부 서비스 상태와 timeout 설정을 확인한다.";
            case "RB-004" -> "DB timeout 발생 시 connection pool 상태와 슬로우 쿼리를 확인한다.";
            default -> "해당 Runbook 내용을 확인하세요.";
        };
    }
}
