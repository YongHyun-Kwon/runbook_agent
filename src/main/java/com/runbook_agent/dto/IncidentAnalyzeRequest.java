package com.runbook_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record IncidentAnalyzeRequest(
        @NotBlank String serviceName,
        @NotBlank String message,
        String timeRange
) {}
