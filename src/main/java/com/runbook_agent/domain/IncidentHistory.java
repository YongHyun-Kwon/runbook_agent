package com.runbook_agent.domain;

public record IncidentHistory(
        String serviceName,
        String occurredAt,
        String incidentId,
        String symptomType,
        String rootCause,
        String resolvedAction
) {}
