package com.runbook_agent.client;

import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseQuery;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrievalConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseVectorSearchConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveRequest;

import java.util.List;
import java.util.Map;

// Phase 5: MockRunbookRetriever를 교체하는 실제 Bedrock 검색 구현체
@Component
@Profile("bedrock")
public class BedrockKnowledgeBaseRetriever implements RunbookRetriever {

    private static final Map<String, String> RUNBOOK_TITLES = Map.of(
            "RB-001", "배포 직후 5xx 증가 대응 절차",
            "RB-002", "응답 지연 대응 절차",
            "RB-003", "외부 API timeout 대응 절차",
            "RB-004", "DB timeout 대응 절차"
    );

    private final BedrockAgentRuntimeClient client;

    @Value("${aws.bedrock.knowledgebase.id}")
    private String knowledgeBaseId;

    @Value("${aws.bedrock.knowledgebase.top-k:5}")
    private int topK;

    public BedrockKnowledgeBaseRetriever(BedrockAgentRuntimeClient client) {
        this.client = client;
    }

    @Override
    public List<SourceDocument> retrieve(String query, SymptomType symptomType) {
        var request = RetrieveRequest.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .retrievalQuery(KnowledgeBaseQuery.builder().text(query).build())
                .retrievalConfiguration(KnowledgeBaseRetrievalConfiguration.builder()
                        .vectorSearchConfiguration(KnowledgeBaseVectorSearchConfiguration.builder()
                                .numberOfResults(topK)
                                .build())
                        .build())
                .build();

        // 같은 Runbook의 여러 chunk가 반환될 수 있으므로 runbookId 기준 중복 제거
        return client.retrieve(request).retrievalResults().stream()
                .map(r -> {
                    String runbookId = extractRunbookId(r.location().s3Location().uri());
                    return new SourceDocument(
                            runbookId,
                            RUNBOOK_TITLES.getOrDefault(runbookId, runbookId),
                            "RUNBOOK"
                    );
                })
                .distinct()
                .limit(3)
                .toList();
    }

    // "s3://bucket/runbooks/RB-001-deployment-5xx.md" → "RB-001"
    private String extractRunbookId(String s3Uri) {
        String filename = s3Uri.substring(s3Uri.lastIndexOf('/') + 1);
        String[] parts = filename.split("-");
        return parts[0] + "-" + parts[1];
    }
}
