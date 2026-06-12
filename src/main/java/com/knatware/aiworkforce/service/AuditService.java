package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.AuditEvent;
import com.knatware.aiworkforce.repository.AuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Records and queries audit-trail events. Kept deliberately small and safe:
 * logging never throws into business logic (failures are swallowed) so auditing
 * can't break the action it's recording.
 */
@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    /** Record an event. Best-effort: never propagates errors to the caller. */
    public void log(String actor, String action, String detail) {
        try {
            repo.save(new AuditEvent(actor == null ? "system" : actor, action, detail));
        } catch (Exception ignored) { }
    }

    /** Most recent events (newest first), capped. */
    public List<AuditEvent> recent(int limit) {
        return repo.findAllByOrderByAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 500)))).getContent();
    }
}
