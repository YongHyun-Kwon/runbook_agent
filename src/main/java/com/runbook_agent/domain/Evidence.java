package com.runbook_agent.domain;

public record Evidence(
        String id,
        String type,
        String sourceId,
        String content
) {}
