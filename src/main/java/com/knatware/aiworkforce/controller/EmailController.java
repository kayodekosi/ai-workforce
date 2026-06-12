package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.Email;
import com.knatware.aiworkforce.model.MailSettings;
import com.knatware.aiworkforce.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Compose emails, view the outbox, and manage/test SMTP settings. */
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService email;

    public EmailController(EmailService email) { this.email = email; }

    @GetMapping
    public List<Email> outbox() { return email.outbox(); }

    @PostMapping
    public Email compose(@RequestBody Map<String, String> body) {
        return email.send(body.get("to"), body.get("subject"), body.get("body"), "manual");
    }

    // --- SMTP settings (portal Settings screen) ---

    @GetMapping("/settings")
    public MailSettings getSettings() {
        MailSettings s = email.settings();
        s.setPassword(null);   // never expose the stored password
        return s;
    }

    @PutMapping("/settings")
    public MailSettings saveSettings(@RequestBody MailSettings incoming) {
        MailSettings saved = email.saveSettings(incoming);
        saved.setPassword(null);
        return saved;
    }

    @PostMapping("/settings/test")
    public EmailService.TestResult test() {
        return email.testConnection();
    }
}
