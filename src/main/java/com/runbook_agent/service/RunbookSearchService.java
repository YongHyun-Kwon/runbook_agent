package com.runbook_agent.service;

import com.runbook_agent.client.RunbookRetriever;
import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RunbookSearchService {

    private final RunbookRetriever runbookRetriever;

    public RunbookSearchService(RunbookRetriever runbookRetriever) {
        this.runbookRetriever = runbookRetriever;
    }

    public List<SourceDocument> search(String query, SymptomType symptomType) {
        return runbookRetriever.retrieve(query, symptomType);
    }
}
