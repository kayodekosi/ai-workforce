package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/**
 * Portal-wide branding and appearance, editable from the Settings screen.
 * Single row (id = 1). Controls the portal name, logo, and default theme/accent.
 */
@Entity
@Table(name = "portal_settings")
public class PortalSettings {
    @Id
    private Long id = 1L;

    private String portalName = "AI-Workforce";
    @Column(length = 2000)
    private String logoUrl;                 // optional image URL or data URI
    private String theme = "midnight";      // midnight (default) | ocean | forest | purple | crimson | slate | light
    private String accent = "blue";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPortalName() { return portalName; }
    public void setPortalName(String portalName) { this.portalName = portalName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }
}
