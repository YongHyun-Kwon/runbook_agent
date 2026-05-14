package com.runbook_agent.client;

import com.runbook_agent.domain.DeploymentHistory;
import com.runbook_agent.domain.ErrorRate;
import com.runbook_agent.domain.IncidentHistory;
import com.runbook_agent.domain.ServiceStatus;
import com.runbook_agent.domain.SymptomType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Phase 7에서 LambdaOpsDataClient로 교체 예정
@Component
public class MockOpsDataClient implements OpsDataClient {

    // 섹션 15.5 edge case 포함 — 단일 지표 과신 방지를 위한 설계
    private static final Map<String, ServiceStatus> SERVICE_STATUS = Map.of(
        "checkout-service", new ServiceStatus("checkout-service", "DEGRADED", "FAIL",  "2026-05-14T10:15:00Z"),
        "payment-service",  new ServiceStatus("payment-service",  "HEALTHY",  "PASS",  "2026-05-14T10:15:00Z"),
        "order-service",    new ServiceStatus("order-service",    "DEGRADED", "FAIL",  "2026-05-14T10:15:00Z")
    );

    private static final Map<String, List<DeploymentHistory>> DEPLOYMENTS = Map.of(
        "checkout-service", List.of(
            new DeploymentHistory("checkout-service", "2026-05-14T09:58:00Z", "v1.24.3", "payment-api timeout handling changed")
        ),
        // edge case: 배포는 있었지만 3시간 전 → 배포 원인 단정 방지
        "payment-service", List.of(
            new DeploymentHistory("payment-service", "2026-05-14T07:15:00Z", "v2.1.0", "minor config update")
        ),
        "order-service", List.of()
    );

    private static final Map<String, List<IncidentHistory>> INCIDENTS = Map.of(
        "checkout-service", List.of(
            new IncidentHistory("checkout-service", "2026-05-10T12:10:00Z", "INC-2026-001",
                "HTTP_5XX_INCREASE", "payment-api timeout", "rollback after approval")
        ),
        "payment-service", List.of(
            new IncidentHistory("payment-service", "2026-05-08T09:30:00Z", "INC-2026-002",
                "EXTERNAL_API_TIMEOUT", "external payment gateway down", "wait and retry")
        ),
        "order-service", List.of(
            new IncidentHistory("order-service", "2026-05-12T14:20:00Z", "INC-2026-003",
                "DB_TIMEOUT", "connection pool exhausted", "pool size increased after approval")
        )
    );

    private static final Map<String, ErrorRate> ERROR_RATES = Map.of(
        "checkout-service", new ErrorRate("checkout-service", 30, 7.8, 1.2, 0.3),
        // edge case: 에러율은 높지만 healthCheck는 정상 → 단일 지표 과신 방지
        "payment-service",  new ErrorRate("payment-service",  30, 0.4, 0.5, 0.3),
        "order-service",    new ErrorRate("order-service",    30, 2.1, 0.8, 0.3)
    );

    @Override
    public ServiceStatus getServiceStatus(String serviceName) {
        return findOrThrow(SERVICE_STATUS, serviceName, "GetServiceStatus");
    }

    @Override
    public List<DeploymentHistory> getRecentDeployments(String serviceName, int withinMinutes) {
        return findOrThrow(DEPLOYMENTS, serviceName, "GetRecentDeployments");
    }

    @Override
    public List<IncidentHistory> getRecentIncidents(String serviceName, SymptomType symptomType, int withinDays) {
        return findOrThrow(INCIDENTS, serviceName, "GetRecentIncidents");
    }

    @Override
    public ErrorRate getErrorRate(String serviceName, int windowMinutes) {
        return findOrThrow(ERROR_RATES, serviceName, "GetErrorRate");
    }

    private <T> T findOrThrow(Map<String, T> map, String serviceName, String action) {
        return Optional.ofNullable(map.get(serviceName))
            .orElseThrow(() -> new IllegalArgumentException(
                action + " failed: service not found [" + serviceName + "]"
            ));
    }
}
