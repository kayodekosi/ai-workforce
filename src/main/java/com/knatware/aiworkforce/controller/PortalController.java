package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.PortalSettings;
import com.knatware.aiworkforce.repository.PortalSettingsRepository;
import org.springframework.web.bind.annotation.*;

/**
 * Portal branding & appearance (name, logo, theme, accent). The GET is public so
 * the login screen and welcome page can brand themselves; writes require auth.
 */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalSettingsRepository repo;

    public PortalController(PortalSettingsRepository repo) { this.repo = repo; }

    @GetMapping("/settings")
    public PortalSettings get() {
        return repo.findById(1L).orElseGet(() -> repo.save(new PortalSettings()));
    }

    @PutMapping("/settings")
    public PortalSettings save(@RequestBody PortalSettings incoming) {
        PortalSettings s = repo.findById(1L).orElseGet(PortalSettings::new);
        s.setId(1L);
        if (incoming.getPortalName() != null) s.setPortalName(incoming.getPortalName());
        s.setLogoUrl(incoming.getLogoUrl());
        if (incoming.getTheme() != null) s.setTheme(incoming.getTheme());
        if (incoming.getAccent() != null) s.setAccent(incoming.getAccent());
        return repo.save(s);
    }
}
