package com.runbook_agent.service;

import com.runbook_agent.domain.SymptomType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// 섹션 13 Action 호출 정책 구현
@Component
public class ActionDecider {

    public List<String> decide(SymptomType symptomType, String message) {
        String lower = message.toLowerCase();
        var actions = new LinkedHashSet<String>();

        switch (symptomType) {
            case HTTP_5XX_INCREASE -> {
                actions.add("GetErrorRate");
                if (hasDeploymentKeyword(lower)) actions.add("GetRecentDeployments");
                if (hasPastIncidentKeyword(lower)) actions.add("GetRecentIncidents");
                if (hasStatusKeyword(lower)) actions.add("GetServiceStatus");
            }
            case LATENCY_INCREASE -> {
                actions.add("GetServiceStatus");
                actions.add("GetErrorRate");
                if (hasExternalKeyword(lower)) actions.add("GetRecentIncidents");
                if (hasDeploymentKeyword(lower)) actions.add("GetRecentDeployments");
            }
            case DB_TIMEOUT -> {
                actions.add("GetServiceStatus");
                actions.add("GetRecentIncidents");
                if (hasErrorRateKeyword(lower)) actions.add("GetErrorRate");
            }
            case DEPLOYMENT_RELATED_FAILURE -> {
                actions.add("GetRecentDeployments");
                actions.add("GetErrorRate");
                if (hasStatusKeyword(lower)) actions.add("GetServiceStatus");
                if (hasPastIncidentKeyword(lower)) actions.add("GetRecentIncidents");
            }
            default -> {
                actions.add("GetServiceStatus");
                actions.add("GetErrorRate");
            }
        }

        return new ArrayList<>(actions);
    }

    private boolean hasDeploymentKeyword(String lower) {
        return containsAny(lower, "배포", "릴리즈", "버전", "deploy", "release");
    }

    private boolean hasPastIncidentKeyword(String lower) {
        return containsAny(lower, "과거", "이전", "유사", "반복");
    }

    private boolean hasStatusKeyword(String lower) {
        return containsAny(lower, "상태", "health");
    }

    private boolean hasExternalKeyword(String lower) {
        return containsAny(lower, "외부", "external");
    }

    private boolean hasErrorRateKeyword(String lower) {
        return containsAny(lower, "에러율", "오류율");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
