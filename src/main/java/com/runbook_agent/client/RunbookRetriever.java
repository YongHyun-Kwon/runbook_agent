package com.runbook_agent.client;

import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;

import java.util.List;

public interface RunbookRetriever {
    List<SourceDocument> retrieve(String query, SymptomType symptomType);
}
