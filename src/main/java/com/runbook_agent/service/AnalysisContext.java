package com.runbook_agent.service;

import com.runbook_agent.domain.Evidence;
import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;

import java.util.List;

public record AnalysisContext(
        String serviceName,
        String message,
        SymptomType symptomType,
        List<SourceDocument> runbooks,
        List<Evidence> allEvidence,
        List<Evidence> opsEvidence
) {}
