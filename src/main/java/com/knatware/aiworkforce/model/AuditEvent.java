package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * An audit-trail record: who did what, when. Captured for security- and
 * HR-relevant actions (logins, staff changes, candidate decisions, interviews,
 * emails, meetings) so activity is traceable.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor;       // username or "system"
    private String action;      // e.g. "LOGIN", "STAFF_ONBOARD", "CANDIDATE_STATUS"
    @Column(length = 1000)
    private String detail;      // human-readable description
    private Instant at = Instant.now();

    public AuditEvent() { }
    public AuditEvent(String actor, String action, String detail) {
        this.actor = actor; this.action = action; this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
