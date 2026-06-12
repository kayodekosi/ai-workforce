package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.Interview;
import com.knatware.aiworkforce.service.InterviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI interview endpoints. Create an interview (generates questions), submit
 * answers, and score it through the connector layer (with simulation fallback).
 */
@RestController
@RequestMapping("/api/hr/interviews")
public class InterviewController {

    private final InterviewService interviews;

    public InterviewController(InterviewService interviews) { this.interviews = interviews; }

    @PostMapping("/create")
    public Interview create(@RequestBody Map<String, Object> body) {
        Long candidateId = ((Number) body.get("candidateId")).longValue();
        return interviews.create(candidateId);
    }

    @GetMapping("/{id}")
    public Interview get(@PathVariable Long id) { return interviews.get(id); }

    @GetMapping("/latest/{candidateId}")
    public Interview latest(@PathVariable Long candidateId) {
        return interviews.latestForCandidate(candidateId);
    }

    @PostMapping("/{id}/answers")
    public Interview submit(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        return interviews.submitAnswers(id, body.getOrDefault("answers", List.of()));
    }

    @PostMapping("/{id}/score")
    public Interview score(@PathVariable Long id) { return interviews.score(id); }
}
