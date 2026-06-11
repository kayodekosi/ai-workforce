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

    public AuthService(AdminUserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
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
}
