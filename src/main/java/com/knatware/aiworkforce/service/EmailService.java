package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.Email;
import com.knatware.aiworkforce.model.MailSettings;
import com.knatware.aiworkforce.repository.EmailRepository;
import com.knatware.aiworkforce.repository.MailSettingsRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Outbox-first email with runtime-configurable SMTP.
 *
 * SMTP settings live in the database (editable from the portal Settings screen),
 * so they can be changed and tested without restarting or editing files. Every
 * email is stored in the outbox; if SMTP is enabled and configured, it is also
 * actually sent. A connectivity test verifies the SMTP server before you rely on it.
 */
@Service
public class EmailService {

    private final EmailRepository emails;
    private final MailSettingsRepository settingsRepo;

    public EmailService(EmailRepository emails, MailSettingsRepository settingsRepo) {
        this.emails = emails;
        this.settingsRepo = settingsRepo;
    }

    public List<Email> outbox() { return emails.findAllByOrderByCreatedAtDesc(); }

    /** Current settings (creates a default singleton row on first access). */
    public MailSettings settings() {
        return settingsRepo.findById(1L).orElseGet(() -> {
            MailSettings s = new MailSettings();
            return settingsRepo.save(s);
        });
    }

    /** Save SMTP settings from the portal. Password left blank = keep existing. */
    public MailSettings saveSettings(MailSettings incoming) {
        MailSettings s = settings();
        s.setEnabled(incoming.isEnabled());
        s.setHost(incoming.getHost());
        s.setPort(incoming.getPort() == null ? 587 : incoming.getPort());
        s.setUsername(incoming.getUsername());
        if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
            s.setPassword(incoming.getPassword());
        }
        if (incoming.getFromAddress() != null && !incoming.getFromAddress().isBlank()) {
            s.setFromAddress(incoming.getFromAddress());
        }
        s.setStartTls(incoming.isStartTls());
        s.setAuth(incoming.isAuth());
        return settingsRepo.save(s);
    }

    /** Build a mail sender from the current settings, or null if not usable. */
    private JavaMailSenderImpl buildSender(MailSettings s) {
        if (s.getHost() == null || s.getHost().isBlank()) return null;
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.getHost());
        sender.setPort(s.getPort() == null ? 587 : s.getPort());
        if (s.getUsername() != null) sender.setUsername(s.getUsername());
        if (s.getPassword() != null) sender.setPassword(s.getPassword());
        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", String.valueOf(s.isAuth()));
        p.put("mail.smtp.starttls.enable", String.valueOf(s.isStartTls()));
        p.put("mail.smtp.connectiontimeout", "8000");
        p.put("mail.smtp.timeout", "8000");
        p.put("mail.smtp.writetimeout", "8000");
        return sender;
    }

    /** Result of a connectivity test. */
    public record TestResult(boolean ok, String message) { }

    /**
     * Verify the SMTP server is reachable and credentials work, without sending
     * a message. Uses the transport's connect() handshake.
     */
    public TestResult testConnection() {
        MailSettings s = settings();
        JavaMailSenderImpl sender = buildSender(s);
        if (sender == null) {
            return new TestResult(false, "No SMTP host configured.");
        }
        try {
            sender.testConnection();   // opens + closes a connection, authenticating
            return new TestResult(true, "Connection successful — SMTP is reachable and credentials are valid.");
        } catch (Exception e) {
            return new TestResult(false, "Connection failed: " + e.getMessage());
        }
    }

    /** Compose + (attempt to) send an email. Always stored; sent if SMTP is enabled. */
    public Email send(String to, String subject, String body, String trigger) {
        MailSettings s = settings();
        Email email = new Email(to, subject, body, trigger == null ? "manual" : trigger);
        email.setFromAddress(s.getFromAddress());

        JavaMailSenderImpl sender = s.isEnabled() ? buildSender(s) : null;
        if (sender != null && to != null && !to.isBlank()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(to);
                msg.setFrom(s.getFromAddress());
                msg.setSubject(subject == null ? "" : subject);
                msg.setText(body == null ? "" : body);
                sender.send(msg);
                email.setStatus(Email.EmailStatus.SENT);
                email.setSentAt(Instant.now());
            } catch (Exception e) {
                email.setStatus(Email.EmailStatus.FAILED);
                email.setError(e.getMessage());
            }
        } else {
            email.setStatus(Email.EmailStatus.PENDING);
        }
        return emails.save(email);
    }
}
