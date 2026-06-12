package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/** One question in an interview, with the candidate's answer and an AI score. */
@Entity
@Table(name = "interview_question")
public class InterviewQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interview_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Interview interview;

    @Column(length = 1000)
    private String question;

    @Column(length = 4000)
    private String answer;

    /** Per-answer AI score 0–100; null until scored. */
    private Integer score;

    @Column(length = 1000)
    private String feedback;   // AI feedback on the answer

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
