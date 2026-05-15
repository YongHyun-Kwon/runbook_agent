package com.runbook_agent.client;

import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("springai")
public class DocumentRunbookRetriever implements RunbookRetriever {

    private final RunbookDocumentStore store;

    public DocumentRunbookRetriever(RunbookDocumentStore store) {
        this.store = store;
    }

    @Override
    public List<SourceDocument> retrieve(String query, SymptomType symptomType) {
        return store.search(query, symptomType, 3);
    }
}
