package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** A chat channel: direct (1:1), group, or conference (multi-party, AI+human). */
@Entity
@Table(name = "chat_channel")
public class ChatChannel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelType type = ChannelType.DIRECT;

    @ManyToMany
    @JoinTable(name = "channel_members",
        joinColumns = @JoinColumn(name = "channel_id"),
        inverseJoinColumns = @JoinColumn(name = "staff_id"))
    private Set<Staff> members = new HashSet<>();

    private Instant createdAt = Instant.now();

    public ChatChannel() { }
    public ChatChannel(String name, ChannelType type) { this.name = name; this.type = type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }
    public Set<Staff> getMembers() { return members; }
    public void setMembers(Set<Staff> members) { this.members = members; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum ChannelType { DIRECT, GROUP, CONFERENCE }
}
