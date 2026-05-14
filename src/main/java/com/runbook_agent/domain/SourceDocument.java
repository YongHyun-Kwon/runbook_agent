package com.runbook_agent.domain;

public record SourceDocument(
        String runbookId,
        String title,
        String sourceType
) {}
