package com.runbook_agent.domain;

public record DeploymentHistory(
        String serviceName,
        String deployedAt,
        String version,
        String changeSummary
) {}
