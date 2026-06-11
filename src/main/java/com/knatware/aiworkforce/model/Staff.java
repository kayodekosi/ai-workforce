package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/**
 * A staff member — AI or human. Almost everything here is configurable at
 * onboarding via the admin portal: identity, role, reporting line, contact
 * details, and (for AI staff) the prompt, model, voice, and which automation
 * backend powers them.
 */
@Entity
@Table(name = "staff")
public class Staff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- identity ---
    private String fullName;
    private String email;
    private String photoUrl;
    private String phoneExtension;

    // --- role ---
    private String position;       // job title
    @Column(length = 1000)
    private String duties;         // free-text duties / function
    private String function;       // short function tag, e.g. "Support Agent"

    @ManyToOne private Department department;
    @ManyToOne private Level level;
    @ManyToOne private Staff reportsTo;   // reporting line (self-referential)

    @Enumerated(EnumType.STRING)
    private StaffType type = StaffType.AI;

    @Enumerated(EnumType.STRING)
    private StaffStatus status = StaffStatus.ACTIVE;

    /** AI staff are always available; humans follow working hours. */
    private boolean alwaysOnline = true;

    // --- AI configuration (ignored for human staff) ---
    @Column(length = 4000)
    private String systemPrompt;          // the persona / instructions
    private String model;                 // e.g. "gpt-4o", "claude-3"
    @Enumerated(EnumType.STRING)
    private ConnectorType connector = ConnectorType.NONE;  // n8n / langflow / dify / flowise
    private String connectorEndpoint;     // webhook/API URL for the chosen backend
    private boolean voiceEnabled = false;
    private String voiceId;               // voice profile id for TTS
    private String voiceProvider;         // e.g. "elevenlabs", "azure-tts"

    public Staff() { }

    // --- getters / setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPhoneExtension() { return phoneExtension; }
    public void setPhoneExtension(String phoneExtension) { this.phoneExtension = phoneExtension; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDuties() { return duties; }
    public void setDuties(String duties) { this.duties = duties; }
    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public Staff getReportsTo() { return reportsTo; }
    public void setReportsTo(Staff reportsTo) { this.reportsTo = reportsTo; }
    public StaffType getType() { return type; }
    public void setType(StaffType type) { this.type = type; }
    public StaffStatus getStatus() { return status; }
    public void setStatus(StaffStatus status) { this.status = status; }
    public boolean isAlwaysOnline() { return alwaysOnline; }
    public void setAlwaysOnline(boolean alwaysOnline) { this.alwaysOnline = alwaysOnline; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public ConnectorType getConnector() { return connector; }
    public void setConnector(ConnectorType connector) { this.connector = connector; }
    public String getConnectorEndpoint() { return connectorEndpoint; }
    public void setConnectorEndpoint(String connectorEndpoint) { this.connectorEndpoint = connectorEndpoint; }
    public boolean isVoiceEnabled() { return voiceEnabled; }
    public void setVoiceEnabled(boolean voiceEnabled) { this.voiceEnabled = voiceEnabled; }
    public String getVoiceId() { return voiceId; }
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
    public String getVoiceProvider() { return voiceProvider; }
    public void setVoiceProvider(String voiceProvider) { this.voiceProvider = voiceProvider; }
}
