package com.runbook_agent.domain;

public record RecommendedStep(
        int order,
        String step,
        StepType type,
        boolean requiresApproval
) {}
