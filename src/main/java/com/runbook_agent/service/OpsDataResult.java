package com.runbook_agent.service;

import com.runbook_agent.domain.CalledAction;
import com.runbook_agent.domain.Evidence;

import java.util.List;

public record OpsDataResult(
        List<CalledAction> calledActions,
        List<Evidence> evidence
) {}
