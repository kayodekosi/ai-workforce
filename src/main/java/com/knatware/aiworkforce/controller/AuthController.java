package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.service.AuthService;
import org.springframework.web.bind.annotation.*;

/** Login + forced first-login password change for human operators. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    public record LoginRequest(String username, String password) {}
    public record ChangePasswordRequest(String username, String oldPassword, String newPassword) {}

    @PostMapping("/login")
    public AuthService.LoginResult login(@RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password());
    }

    @PostMapping("/change-password")
    public AuthService.LoginResult changePassword(@RequestBody ChangePasswordRequest req) {
        return auth.changePassword(req.username(), req.oldPassword(), req.newPassword());
    }
}
