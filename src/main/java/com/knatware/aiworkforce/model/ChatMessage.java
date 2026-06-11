package com.knatware.aiworkforce.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A single message in a channel. AI replies are produced via the sender's connector. */
@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ChatChannel channel;

    @ManyToOne(optional = false)
    private Staff sender;

    @Column(length = 8000)
    private String content;

    /** Optional URL to a synthesized voice clip, when the sender has voice enabled. */
    private String voiceClipUrl;

    private Instant sentAt = Instant.now();

    public ChatMessage() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChatChannel getChannel() { return channel; }
    public void setChannel(ChatChannel channel) { this.channel = channel; }
    public Staff getSender() { return sender; }
    public void setSender(Staff sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVoiceClipUrl() { return voiceClipUrl; }
    public void setVoiceClipUrl(String voiceClipUrl) { this.voiceClipUrl = voiceClipUrl; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
