package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.AuditEvent;
import com.knatware.aiworkforce.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read the audit trail. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    public List<AuditEvent> recent(@RequestParam(defaultValue = "200") int limit) {
        return audit.recent(limit);
    }
}
