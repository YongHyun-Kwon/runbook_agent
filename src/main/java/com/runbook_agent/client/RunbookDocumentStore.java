package com.runbook_agent.client;

import com.runbook_agent.domain.SourceDocument;
import com.runbook_agent.domain.SymptomType;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Profile("springai")
public class RunbookDocumentStore {

    private record RunbookEntry(String runbookId, String title, List<String> symptomTypes, String fullText) {}

    private final List<RunbookEntry> entries = new ArrayList<>();

    @PostConstruct
    public void loadAll() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:runbooks/*.md");
        for (Resource resource : resources) {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            parse(content).ifPresent(entries::add);
        }
    }

    public Optional<String> getExcerpt(String runbookId) {
        return entries.stream()
                .filter(e -> e.runbookId().equals(runbookId))
                .map(e -> e.fullText().substring(0, Math.min(500, e.fullText().length())))
                .findFirst();
    }

    public List<SourceDocument> search(String query, SymptomType symptomType, int topK) {
        return entries.stream()
                .map(e -> Map.entry(e, score(e, query, symptomType)))
                .filter(kv -> kv.getValue() > 0)
                .sorted(Comparator.comparingDouble(kv -> -kv.getValue()))
                .limit(topK)
                .map(kv -> new SourceDocument(kv.getKey().runbookId(), kv.getKey().title(), "RUNBOOK"))
                .toList();
    }

    private Optional<RunbookEntry> parse(String content) {
        if (!content.startsWith("---")) return Optional.empty();
        int end = content.indexOf("---", 3);
        if (end < 0) return Optional.empty();

        String frontmatter = content.substring(3, end);
        String body = content.substring(end + 3).strip();

        String runbookId = extractField(frontmatter, "runbook_id");
        String title = extractField(frontmatter, "title");
        List<String> symptomTypes = extractList(frontmatter, "symptom_types");

        if (runbookId.isEmpty() || title.isEmpty()) return Optional.empty();

        return Optional.of(new RunbookEntry(runbookId, title, symptomTypes, title + "\n" + body));
    }

    private String extractField(String frontmatter, String key) {
        return Arrays.stream(frontmatter.split("\n"))
                .filter(line -> line.startsWith(key + ":"))
                .map(line -> line.substring(line.indexOf(':') + 1).strip())
                .findFirst()
                .orElse("");
    }

    private List<String> extractList(String frontmatter, String key) {
        List<String> result = new ArrayList<>();
        boolean inSection = false;
        for (String line : frontmatter.split("\n")) {
            if (line.startsWith(key + ":")) {
                inSection = true;
            } else if (inSection && line.strip().startsWith("- ")) {
                result.add(line.strip().substring(2).strip());
            } else if (inSection && !line.strip().isEmpty() && !line.strip().startsWith("-")) {
                break;
            }
        }
        return result;
    }

    // symptomType 일치: +10 (강한 신호), 키워드 포함 횟수: 보조 신호
    private double score(RunbookEntry entry, String query, SymptomType symptomType) {
        double symptomBoost = entry.symptomTypes().contains(symptomType.name()) ? 10.0 : 0.0;
        String[] terms = query.toLowerCase().split("[\\s,!?.]+");
        String text = entry.fullText().toLowerCase();
        double keywordScore = Arrays.stream(terms)
                .filter(t -> t.length() > 2)
                .filter(text::contains)
                .count();
        return symptomBoost + keywordScore;
    }
}
