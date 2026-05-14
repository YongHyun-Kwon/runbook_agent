package com.runbook_agent.domain;

public record IncidentSummary(
        String serviceName,
        SymptomType symptomType,
        Severity severity,
        String timeRange
) {}
