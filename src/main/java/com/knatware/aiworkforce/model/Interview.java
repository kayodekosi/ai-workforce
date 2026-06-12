package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An AI interview for a candidate: a set of questions, the candidate's answers,
 * and AI-produced scores. Scoring runs through the platform's connector layer
 * (the same n8n/Dify/Langflow backends the AI staff use), with a simulation
 * fallback when no backend is configured.
 */
@Entity
@Table(name = "interview")
public class Interview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status = InterviewStatus.CREATED;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "ordinal")
    private List<InterviewQuestion> questions = new ArrayList<>();
    private Integer overallScore;

    @Column(length = 3000)
    private String summary;        // AI overall assessment

    private String connectorUsed;  // which backend produced the scores (or "simulated")
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public List<InterviewQuestion> getQuestions() { return questions; }
    public void setQuestions(List<InterviewQuestion> questions) { this.questions = questions; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getConnectorUsed() { return connectorUsed; }
    public void setConnectorUsed(String connectorUsed) { this.connectorUsed = connectorUsed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum InterviewStatus { CREATED, IN_PROGRESS, ANSWERED, SCORED }
}
