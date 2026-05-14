package com.runbook_agent.domain;

public record ErrorRate(
        String serviceName,
        int windowMinutes,
        double http5xxRate,
        double http4xxRate,
        double baseline5xxRate
) {}
