package com.runbook_agent.dto;

import com.runbook_agent.domain.CalledAction;
import com.runbook_agent.domain.Evidence;
import com.runbook_agent.domain.RecommendedStep;
import com.runbook_agent.domain.RootCauseCandidate;
import com.runbook_agent.domain.IncidentSummary;
import com.runbook_agent.domain.SourceDocument;

import java.util.List;

public record IncidentAnalyzeResponse(
        String requestId,
        IncidentSummary summary,
        List<CalledAction> calledActions,
        List<Evidence> evidence,
        List<RootCauseCandidate> rootCauseCandidates,
        List<RecommendedStep> recommendedSteps,
        List<SourceDocument> sources,
        List<String> warnings
) {}
