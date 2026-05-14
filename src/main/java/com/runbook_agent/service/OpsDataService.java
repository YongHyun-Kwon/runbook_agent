package com.runbook_agent.service;

import com.runbook_agent.client.OpsDataClient;
import com.runbook_agent.domain.CalledAction;
import com.runbook_agent.domain.DeploymentHistory;
import com.runbook_agent.domain.ErrorRate;
import com.runbook_agent.domain.Evidence;
import com.runbook_agent.domain.IncidentHistory;
import com.runbook_agent.domain.ServiceStatus;
import com.runbook_agent.domain.SymptomType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpsDataService {

    private final OpsDataClient opsDataClient;

    public OpsDataService(OpsDataClient opsDataClient) {
        this.opsDataClient = opsDataClient;
    }

    public OpsDataResult fetch(String serviceName, List<String> actionNames, SymptomType symptomType) {
        List<CalledAction> calledActions = new ArrayList<>();
        List<Evidence> evidence = new ArrayList<>();
        int[] counter = {1};

        for (String actionName : actionNames) {
            try {
                String content = switch (actionName) {
                    case "GetServiceStatus" -> formatStatus(opsDataClient.getServiceStatus(serviceName));
                    case "GetRecentDeployments" -> formatDeployments(opsDataClient.getRecentDeployments(serviceName, 60));
                    case "GetRecentIncidents" -> formatIncidents(opsDataClient.getRecentIncidents(serviceName, symptomType, 30));
                    case "GetErrorRate" -> formatErrorRate(opsDataClient.getErrorRate(serviceName, 30));
                    default -> throw new IllegalArgumentException("Unknown action: " + actionName);
                };

                String evidenceId = String.format("OPS-EVIDENCE-%03d", counter[0]++);
                evidence.add(new Evidence(evidenceId, "ACTION_RESULT", actionName, content));
                calledActions.add(new CalledAction(actionName, "SUCCESS", getActionReason(actionName)));

            } catch (Exception e) {
                calledActions.add(new CalledAction(actionName, "FAILED", e.getMessage()));
            }
        }

        return new OpsDataResult(calledActions, evidence);
    }

    private String formatStatus(ServiceStatus s) {
        return String.format("%s 현재 상태: %s (healthCheck: %s, 확인 시각: %s)",
            s.serviceName(), s.status(), s.healthCheck(), s.lastCheckedAt());
    }

    private String formatDeployments(List<DeploymentHistory> deployments) {
        if (deployments.isEmpty()) return "최근 60분 이내 배포 이력 없음";
        var sb = new StringBuilder();
        for (var d : deployments) {
            sb.append(String.format("배포: %s (%s), 변경 내용: %s | ", d.version(), d.deployedAt(), d.changeSummary()));
        }
        return sb.toString().stripTrailing();
    }

    private String formatIncidents(List<IncidentHistory> incidents) {
        if (incidents.isEmpty()) return "최근 30일 이내 유사 장애 이력 없음";
        var sb = new StringBuilder();
        for (var i : incidents) {
            sb.append(String.format("[%s] %s (원인: %s, 조치: %s) | ",
                i.incidentId(), i.symptomType(), i.rootCause(), i.resolvedAction()));
        }
        return sb.toString().stripTrailing();
    }

    private String formatErrorRate(ErrorRate e) {
        double ratio = e.baseline5xxRate() > 0 ? e.http5xxRate() / e.baseline5xxRate() : 0;
        return String.format("최근 %d분 5xx 에러율: %.1f%% (baseline: %.1f%%, %.1f배)",
            e.windowMinutes(), e.http5xxRate(), e.baseline5xxRate(), ratio);
    }

    private String getActionReason(String actionName) {
        return switch (actionName) {
            case "GetServiceStatus"      -> "서비스 현재 상태 확인";
            case "GetRecentDeployments"  -> "최근 배포 영향 확인";
            case "GetRecentIncidents"    -> "유사 장애 이력 확인";
            case "GetErrorRate"          -> "에러율 추이 확인";
            default                      -> actionName;
        };
    }
}
