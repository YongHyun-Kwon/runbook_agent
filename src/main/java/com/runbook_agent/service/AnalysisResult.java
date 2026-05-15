package com.runbook_agent.service;

import com.runbook_agent.domain.RecommendedStep;
import com.runbook_agent.domain.RootCauseCandidate;

import java.util.List;

public record AnalysisResult(
        List<RootCauseCandidate> candidates,
        List<RecommendedStep> steps
) {}
