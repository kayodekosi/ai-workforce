package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.Candidate;
import com.knatware.aiworkforce.model.CandidateStatus;
import com.knatware.aiworkforce.service.HrService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HR department endpoints: manage recruitment candidates, run a (simulated)
 * automated AI screen, grade, and move candidates through the pipeline.
 */
@RestController
@RequestMapping("/api/hr/candidates")
public class HrController {

    private final HrService hr;

    public HrController(HrService hr) { this.hr = hr; }

    @GetMapping
    public List<Candidate> list() { return hr.all(); }

    @PostMapping
    public Candidate add(@RequestBody Candidate c) { return hr.add(c); }

    @PostMapping("/{id}/status")
    public Candidate setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return hr.updateStatus(id, CandidateStatus.valueOf(body.get("status")));
    }

    @PostMapping("/{id}/grade")
    public Candidate grade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int grade = body.get("grade") == null ? 0 : ((Number) body.get("grade")).intValue();
        String notes = body.get("notes") == null ? null : body.get("notes").toString();
        return hr.grade(id, grade, notes);
    }

    @PostMapping("/{id}/ai-screen")
    public Candidate aiScreen(@PathVariable Long id) { return hr.aiScreen(id); }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) { hr.remove(id); }
}
