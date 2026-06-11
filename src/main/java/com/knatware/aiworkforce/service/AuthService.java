package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.model.AdminUser;
import com.knatware.aiworkforce.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles login verification and the first-login forced password change.
 * The seeded default admin (admin/admin) has mustChangePassword=true and cannot
 * be considered fully active until it sets a new password.
 */
@Service
public class AuthService {

    private final AdminUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AdminUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public record LoginResult(boolean ok, boolean mustChangePassword, String message) { }

    public LoginResult login(String username, String password) {
        return users.findByUsername(username)
            .filter(u -> encoder.matches(password, u.getPasswordHash()))
            .map(u -> new LoginResult(true, u.isMustChangePassword(),
                    u.isMustChangePassword()
                        ? "Login OK — you must change your password before continuing."
                        : "Login OK."))
            .orElse(new LoginResult(false, false, "Invalid username or password."));
    }

    public LoginResult changePassword(String username, String oldPassword, String newPassword) {
        AdminUser u = users.findByUsername(username).orElse(null);
        if (u == null || !encoder.matches(oldPassword, u.getPasswordHash())) {
            return new LoginResult(false, false, "Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return new LoginResult(false, true, "New password must be at least 8 characters.");
        }
        if (encoder.matches(newPassword, u.getPasswordHash())) {
            return new LoginResult(false, true, "New password must differ from the current one.");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        users.save(u);
        return new LoginResult(true, false, "Password changed successfully.");
    }
}
