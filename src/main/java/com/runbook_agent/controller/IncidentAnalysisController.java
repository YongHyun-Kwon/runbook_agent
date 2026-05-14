package com.runbook_agent.controller;

import com.runbook_agent.dto.IncidentAnalyzeRequest;
import com.runbook_agent.dto.IncidentAnalyzeResponse;
import com.runbook_agent.service.IncidentAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentAnalysisController {

    private final IncidentAnalysisService analysisService;

    public IncidentAnalysisController(IncidentAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<IncidentAnalyzeResponse> analyze(@Valid @RequestBody IncidentAnalyzeRequest request) {
        return ResponseEntity.ok(analysisService.analyze(request));
    }
}
