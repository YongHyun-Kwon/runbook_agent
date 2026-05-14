package com.runbook_agent.domain;

public record CalledAction(
        String name,
        String status,
        String reason
) {}
