package com.runbook_agent.client;

import com.runbook_agent.domain.DeploymentHistory;
import com.runbook_agent.domain.ErrorRate;
import com.runbook_agent.domain.IncidentHistory;
import com.runbook_agent.domain.ServiceStatus;
import com.runbook_agent.domain.SymptomType;

import java.util.List;

public interface OpsDataClient {
    ServiceStatus getServiceStatus(String serviceName);
    List<DeploymentHistory> getRecentDeployments(String serviceName, int withinMinutes);
    List<IncidentHistory> getRecentIncidents(String serviceName, SymptomType symptomType, int withinDays);
    ErrorRate getErrorRate(String serviceName, int windowMinutes);
}
