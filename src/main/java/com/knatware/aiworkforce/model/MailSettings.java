package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/**
 * Persisted SMTP configuration, editable from the portal Settings screen.
 * A single row (id = 1) holds the active config so it survives restarts and
 * doesn't require editing properties files.
 */
@Entity
@Table(name = "mail_settings")
public class MailSettings {
    @Id
    private Long id = 1L;       // singleton row

    private boolean enabled = false;
    private String host;
    private Integer port = 587;
    private String username;
    private String password;
    private String fromAddress = "no-reply@knatware.com";
    private boolean startTls = true;
    private boolean auth = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public boolean isStartTls() { return startTls; }
    public void setStartTls(boolean startTls) { this.startTls = startTls; }
    public boolean isAuth() { return auth; }
    public void setAuth(boolean auth) { this.auth = auth; }
}
