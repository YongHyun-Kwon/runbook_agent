package com.runbook_agent.domain;

import java.util.List;

public record RootCauseCandidate(
        String name,
        Confidence confidence,
        List<String> evidenceRefs,
        String uncertainty
) {}
