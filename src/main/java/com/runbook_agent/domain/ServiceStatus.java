package com.runbook_agent.domain;

public record ServiceStatus(
        String serviceName,
        String status,
        String healthCheck,
        String lastCheckedAt
) {}
