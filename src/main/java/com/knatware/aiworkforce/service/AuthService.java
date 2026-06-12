package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.AdminUser;
import com.knatware.aiworkforce.repository.AdminUserRepository;
import com.knatware.aiworkforce.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles login verification, JWT issuance, and the first-login forced password
 * change. The seeded default admin (admin/admin) has mustChangePassword=true and
 * is not issued a working token until it sets a new password.
 */
@Service
public class AuthService {

    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthService(AdminUserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    /** token is null when login fails or a password change is required first. */
    public record LoginResult(boolean ok, boolean mustChangePassword, String token,
                              String role, String message) { }

    public LoginResult login(String username, String password) {
        AdminUser u = users.findByUsername(username).orElse(null);
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            return new LoginResult(false, false, null, null, "Invalid username or password.");
        }
        if (u.isMustChangePassword()) {
            // Authenticated, but must change password before receiving a token.
            return new LoginResult(true, true, null, u.getRole(),
                    "Login OK — you must change your password before continuing.");
        }
        String token = jwt.issue(u.getUsername(), u.getRole());
        audit.log(u.getUsername(), "LOGIN", "Successful login");
        return new LoginResult(true, false, token, u.getRole(), "Login OK.");
    }

    public LoginResult changePassword(String username, String oldPassword, String newPassword) {
        AdminUser u = users.findByUsername(username).orElse(null);
        if (u == null || !encoder.matches(oldPassword, u.getPasswordHash())) {
            return new LoginResult(false, false, null, null, "Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return new LoginResult(false, true, null, u.getRole(),
                    "New password must be at least 8 characters.");
        }
        if (encoder.matches(newPassword, u.getPasswordHash())) {
            return new LoginResult(false, true, null, u.getRole(),
                    "New password must differ from the current one.");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        users.save(u);
        // Now fully active — issue a token so they're logged straight in.
        String token = jwt.issue(u.getUsername(), u.getRole());
        return new LoginResult(true, false, token, u.getRole(), "Password changed successfully.");
    }

    public java.util.Map<String,String> getPrefs(String username) {
        AdminUser u = users.findByUsername(username).orElse(null);
        String theme = u != null && u.getTheme() != null ? u.getTheme() : "midnight";
        String accent = u != null && u.getAccent() != null ? u.getAccent() : "blue";
        return java.util.Map.of("theme", theme, "accent", accent);
    }

    public java.util.Map<String,String> savePrefs(String username, String theme, String accent) {
        AdminUser u = users.findByUsername(username).orElse(null);
        if (u != null) {
            if (theme != null) u.setTheme(theme);
            if (accent != null) u.setAccent(accent);
            users.save(u);
        }
        return getPrefs(username);
    }
}
