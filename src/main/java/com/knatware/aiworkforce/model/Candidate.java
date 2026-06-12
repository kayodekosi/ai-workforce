package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A recruitment candidate tracked by the HR department. This is a working
 * starter for an applicant pipeline: candidates move through stages and can be
 * graded. Automated sourcing / AI interviewing / scoring are roadmap extensions
 * that plug into this model (see HrService).
 */
@Entity
@Table(name = "candidate")
public class Candidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String roleAppliedFor;

    @ManyToOne
    private Department department;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status = CandidateStatus.APPLIED;

    /** Overall grade 0–100 (e.g. from screening / AI interview). Null until graded. */
    private Integer grade;

    @Column(length = 2000)
    private String notes;

    private Instant appliedAt = Instant.now();

    public Candidate() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoleAppliedFor() { return roleAppliedFor; }
    public void setRoleAppliedFor(String roleAppliedFor) { this.roleAppliedFor = roleAppliedFor; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public CandidateStatus getStatus() { return status; }
    public void setStatus(CandidateStatus status) { this.status = status; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
