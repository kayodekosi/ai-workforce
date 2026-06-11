package com.knatware.aiworkforce.model;

import jakarta.persistence.*;

/**
 * A portal login account. These are the HUMAN users who administer or operate
 * the platform (AI staff don't log in — they're operated/simulated). The seeded
 * default admin must change its password at first login.
 */
@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String role = "ADMIN";          // ADMIN or OPERATOR
    private boolean mustChangePassword = true;

    /** Optionally link a login to a human Staff record. */
    @OneToOne
    private Staff staff;

    public AdminUser() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
}
