package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.connector.AiConnector;
import com.knatware.aiworkforce.connector.AiConnectorRegistry;
import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.CandidateRepository;
import com.knatware.aiworkforce.repository.InterviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AI interview engine.
 *
 * Generates a set of role-aware questions for a candidate, records their
 * answers, and scores the interview. Scoring runs through the platform's
 * {@link AiConnectorRegistry} — the SAME connector layer (n8n, Dify, Langflow,
 * Flowise, custom webhook) that powers AI staff — by constructing a synthetic
 * "AI interviewer" agent whose backend/endpoint come from configuration. When
 * no backend is configured (or a call fails), it falls back to a deterministic
 * local heuristic so the engine always works in a demo.
 */
@Service
public class InterviewService {

    private final InterviewRepository interviews;
    private final CandidateRepository candidates;
    private final AiConnectorRegistry connectors;
    private final EmailService email;

    /** Optional shared backend for the AI interviewer (else simulation is used). */
    private final ConnectorType interviewerConnector;
    private final String interviewerEndpoint;
    private final String interviewerModel;

    public InterviewService(InterviewRepository interviews, CandidateRepository candidates,
                            AiConnectorRegistry connectors, EmailService email,
                            @Value("${app.hr.interviewer.connector:NONE}") String connector,
                            @Value("${app.hr.interviewer.endpoint:}") String endpoint,
                            @Value("${app.hr.interviewer.model:}") String model) {
        this.interviews = interviews;
        this.candidates = candidates;
        this.connectors = connectors;
        this.email = email;
        ConnectorType ct;
        try { ct = ConnectorType.valueOf(connector); } catch (Exception e) { ct = ConnectorType.NONE; }
        this.interviewerConnector = ct;
        this.interviewerEndpoint = endpoint;
        this.interviewerModel = model;
    }

    /** Create an interview with role-aware questions for a candidate. */
    @Transactional
    public Interview create(Long candidateId) {
        Candidate c = candidates.findById(candidateId).orElseThrow();
        Interview iv = new Interview();
        iv.setCandidate(c);
        iv.setStatus(Interview.InterviewStatus.CREATED);

        for (String q : generateQuestions(c)) {
            InterviewQuestion iq = new InterviewQuestion();
            iq.setInterview(iv);
            iq.setQuestion(q);
            iv.getQuestions().add(iq);
        }
        return interviews.save(iv);
    }

    public Interview get(Long interviewId) {
        return interviews.findById(interviewId).orElseThrow();
    }

    public Interview latestForCandidate(Long candidateId) {
        return interviews.findFirstByCandidateIdOrderByCreatedAtDesc(candidateId).orElse(null);
    }

    /** Save the candidate's answers (list aligned to question order). */
    @Transactional
    public Interview submitAnswers(Long interviewId, List<String> answers) {
        Interview iv = get(interviewId);
        List<InterviewQuestion> qs = iv.getQuestions();
        for (int i = 0; i < qs.size() && i < answers.size(); i++) {
            qs.get(i).setAnswer(answers.get(i));
        }
        iv.setStatus(Interview.InterviewStatus.ANSWERED);
        return interviews.save(iv);
    }

    /**
     * Score the interview. Each answer is scored 0–100 with short feedback, and
     * an overall score + summary is produced. Runs via the connector layer when
     * configured; otherwise uses a deterministic local heuristic.
     */
    @Transactional
    public Interview score(Long interviewId) {
        Interview iv = get(interviewId);

        AiConnector connector = resolveConnector();
        boolean simulated = (connector == null);
        iv.setConnectorUsed(simulated ? "simulated" : interviewerConnector.name());

        int total = 0, counted = 0;
        for (InterviewQuestion q : iv.getQuestions()) {
            int s;
            String fb;
            if (q.getAnswer() == null || q.getAnswer().isBlank()) {
                s = 0; fb = "No answer provided.";
            } else if (simulated) {
                s = heuristicScore(q.getAnswer());
                fb = heuristicFeedback(s);
            } else {
                int[] holder = {-1};
                fb = scoreViaConnector(connector, q.getQuestion(), q.getAnswer(), holder);
                s = holder[0] >= 0 ? holder[0] : heuristicScore(q.getAnswer());
            }
            q.setScore(s);
            q.setFeedback(fb);
            total += s; counted++;
        }

        int overall = counted == 0 ? 0 : Math.round((float) total / counted);
        iv.setOverallScore(overall);
        iv.setSummary(overall >= 70
                ? "Strong candidate — recommended to shortlist."
                : overall >= 50
                    ? "Borderline — consider a follow-up interview."
                    : "Below threshold on this interview.");
        iv.setStatus(Interview.InterviewStatus.SCORED);

        // reflect result on the candidate record
        Candidate c = iv.getCandidate();
        c.setGrade(overall);
        c.setStatus(overall >= 70 ? CandidateStatus.SHORTLISTED : CandidateStatus.AI_INTERVIEW);
        candidates.save(c);

        // notify the candidate of their interview result
        if (c.getEmail() != null && !c.getEmail().isBlank()) {
            String subject = "Your interview result";
            String body = "Hi " + c.getFullName() + ",\n\nThank you for completing your AI interview"
                    + (c.getRoleAppliedFor() != null ? " for " + c.getRoleAppliedFor() : "") + ".\n\n"
                    + iv.getSummary() + "\n\nKnatware AI Company Ltd";
            email.send(c.getEmail(), subject, body, "interview-scored");
        }

        return interviews.save(iv);
    }

    // ---------- internals ----------

    private List<String> generateQuestions(Candidate c) {
        String role = c.getRoleAppliedFor() == null ? "this role" : c.getRoleAppliedFor();
        List<String> qs = new ArrayList<>();
        qs.add("Tell us about your background and why you're interested in " + role + ".");
        qs.add("Describe a challenging problem you solved relevant to " + role + ". What was your approach?");
        qs.add("How do you prioritise when you have multiple competing deadlines?");
        qs.add("Give an example of working with a team to achieve a goal. What was your contribution?");
        qs.add("Where do you want to grow, and how does " + role + " fit your goals?");
        return qs;
    }

    /** Build a synthetic AI-interviewer agent and route through the connector layer. */
    private AiConnector resolveConnector() {
        if (interviewerConnector == ConnectorType.NONE
                || interviewerEndpoint == null || interviewerEndpoint.isBlank()) {
            return null; // simulation
        }
        Staff interviewer = new Staff();
        interviewer.setFullName("AI Interviewer");
        interviewer.setType(StaffType.AI);
        interviewer.setConnector(interviewerConnector);
        interviewer.setConnectorEndpoint(interviewerEndpoint);
        interviewer.setModel(interviewerModel);
        interviewer.setSystemPrompt(
                "You are a fair, rigorous technical interviewer. Score the candidate's answer "
              + "from 0 to 100 and give one line of feedback. Respond as: SCORE: <n> | <feedback>.");
        return connectors.forStaff(interviewer);
    }

    private static final Pattern SCORE_PAT = Pattern.compile("(\\d{1,3})");

    private String scoreViaConnector(AiConnector connector, String question, String answer, int[] outScore) {
        try {
            String prompt = "Question: " + question + "\nAnswer: " + answer
                    + "\nScore 0-100 and give one line of feedback as 'SCORE: <n> | <feedback>'.";
            AiConnector.AiReply reply = connector.generateReply(
                    interviewerStaff(), prompt);
            String text = reply.text() == null ? "" : reply.text();
            Matcher m = SCORE_PAT.matcher(text);
            if (m.find()) {
                int n = Integer.parseInt(m.group(1));
                outScore[0] = Math.max(0, Math.min(100, n));
            }
            int bar = text.indexOf('|');
            return bar >= 0 ? text.substring(bar + 1).trim() : text.trim();
        } catch (Exception e) {
            return "Scored by fallback heuristic (" + e.getMessage() + ").";
        }
    }

    private Staff interviewerStaff() {
        Staff s = new Staff();
        s.setFullName("AI Interviewer");
        s.setType(StaffType.AI);
        s.setConnector(interviewerConnector);
        s.setConnectorEndpoint(interviewerEndpoint);
        s.setModel(interviewerModel);
        return s;
    }

    private int heuristicScore(String answer) {
        // deterministic, length- and content-aware placeholder (repeatable)
        int len = answer.trim().length();
        int base = Math.min(60, len / 8);                 // longer, more developed answers score higher
        int variety = (int) (answer.toLowerCase().chars().distinct().count()); // 0..~30
        int score = 30 + base + Math.min(20, variety / 2);
        return Math.max(0, Math.min(100, score));
    }

    private String heuristicFeedback(int s) {
        if (s >= 80) return "Clear, well-developed answer.";
        if (s >= 60) return "Solid answer; could add more specifics.";
        if (s >= 40) return "Adequate but lacks depth.";
        return "Answer is too brief or unfocused.";
    }
}
