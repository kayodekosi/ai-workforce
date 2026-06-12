package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.Candidate;
import com.knatware.aiworkforce.model.CandidateStatus;
import com.knatware.aiworkforce.repository.CandidateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * HR department logic — a working starter for an automated recruitment pipeline.
 *
 * Candidates are tracked through stages and can be graded. The {@link #aiScreen}
 * method demonstrates the automated-assessment hook: it produces a screening
 * grade and advances the candidate. It currently uses a deterministic local
 * heuristic (no external call) so it runs with zero setup; in production this is
 * where you'd call an AI interview/scoring workflow (e.g. via the same connector
 * layer the AI staff use) — the method signature and pipeline stay the same.
 */
@Service
public class HrService {

    private final CandidateRepository candidates;
    private final EmailService email;

    public HrService(CandidateRepository candidates, EmailService email) {
        this.candidates = candidates;
        this.email = email;
    }

    public List<Candidate> all() { return candidates.findAllByOrderByAppliedAtDesc(); }

    public Candidate add(Candidate c) {
        c.setId(null);
        if (c.getStatus() == null) c.setStatus(CandidateStatus.APPLIED);
        return candidates.save(c);
    }

    public Candidate updateStatus(Long id, CandidateStatus status) {
        Candidate c = candidates.findById(id).orElseThrow();
        c.setStatus(status);
        Candidate saved = candidates.save(c);
        notifyStatus(saved);
        return saved;
    }

    /** Auto-email the candidate when they reach a notable stage. */
    private void notifyStatus(Candidate c) {
        if (c.getEmail() == null || c.getEmail().isBlank()) return;
        String subject, body;
        switch (c.getStatus()) {
            case SHORTLISTED -> { subject = "You've been shortlisted";
                body = "Hi " + c.getFullName() + ",\n\nGreat news — you've been shortlisted for "
                     + safeRole(c) + ". We'll be in touch about next steps.\n\nKnatware AI Company Ltd"; }
            case OFFER -> { subject = "An offer for you";
                body = "Hi " + c.getFullName() + ",\n\nWe're delighted to extend an offer for "
                     + safeRole(c) + ". Details to follow.\n\nKnatware AI Company Ltd"; }
            case HIRED -> { subject = "Welcome to the team!";
                body = "Hi " + c.getFullName() + ",\n\nWelcome aboard! We're excited to have you join us for "
                     + safeRole(c) + ".\n\nKnatware AI Company Ltd"; }
            case REJECTED -> { subject = "Update on your application";
                body = "Hi " + c.getFullName() + ",\n\nThank you for your interest in " + safeRole(c)
                     + ". On this occasion we won't be moving forward, but we wish you well.\n\nKnatware AI Company Ltd"; }
            default -> { return; } // no email for early/neutral stages
        }
        email.send(c.getEmail(), subject, body, "candidate-status");
    }

    private String safeRole(Candidate c) {
        return c.getRoleAppliedFor() == null ? "the role" : c.getRoleAppliedFor();
    }

    public Candidate grade(Long id, int grade, String notes) {
        Candidate c = candidates.findById(id).orElseThrow();
        c.setGrade(Math.max(0, Math.min(100, grade)));
        if (notes != null) c.setNotes(notes);
        return candidates.save(c);
    }

    public void remove(Long id) { candidates.deleteById(id); }

    /**
     * Demonstrates automated AI screening: assigns a grade and advances the
     * candidate to AI_INTERVIEW (or SHORTLISTED if they score highly). The grade
     * here is a deterministic placeholder; replace the scoring line with a call
     * to your AI interview/grading workflow to make it real.
     */
    public Candidate aiScreen(Long id) {
        Candidate c = candidates.findById(id).orElseThrow();

        // --- placeholder scoring (deterministic, no external call) ---
        int score = simulatedScore(c);
        c.setGrade(score);
        c.setStatus(score >= 70 ? CandidateStatus.SHORTLISTED : CandidateStatus.AI_INTERVIEW);
        c.setNotes((c.getNotes() == null ? "" : c.getNotes() + "\n")
                + "Auto-screen score: " + score + "/100 (simulated).");
        return candidates.save(c);
    }

    private int simulatedScore(Candidate c) {
        // Stable pseudo-score from the candidate's details so results are repeatable.
        String basis = (c.getFullName() == null ? "" : c.getFullName())
                + (c.getRoleAppliedFor() == null ? "" : c.getRoleAppliedFor());
        int h = Math.abs(basis.hashCode());
        return 55 + (h % 41); // 55..95
    }
}
