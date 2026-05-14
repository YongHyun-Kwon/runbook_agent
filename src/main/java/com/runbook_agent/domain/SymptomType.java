package com.runbook_agent.domain;

public enum SymptomType {
    HTTP_5XX_INCREASE,
    LATENCY_INCREASE,
    DB_TIMEOUT,
    EXTERNAL_API_TIMEOUT,
    DEPLOYMENT_RELATED_FAILURE,
    UNKNOWN
}
