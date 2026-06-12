package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.connector.AiConnector;
import com.knatware.aiworkforce.connector.AiConnectorRegistry;
import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Core chat logic. Supports direct, group, and conference channels. When a
 * message is posted, every AI staff member in the channel (other than the
 * sender) generates a reply through its configured connector — this is how AI
 * staff "talk to each other" and how the admin simulates conversations.
 *
 * Each saved message (the human/admin message and every AI reply) is also
 * broadcast over WebSockets to /topic/channels/{id} so connected clients update
 * in real time.
 */
@Service
public class ChatService {

    private final ChatChannelRepository channels;
    private final ChatMessageRepository messages;
    private final StaffRepository staff;
    private final AiConnectorRegistry connectors;
    private final SimpMessagingTemplate broker;
    private final com.knatware.aiworkforce.voice.VoiceService voice;

    public ChatService(ChatChannelRepository channels, ChatMessageRepository messages,
                       StaffRepository staff, AiConnectorRegistry connectors,
                       SimpMessagingTemplate broker,
                       com.knatware.aiworkforce.voice.VoiceService voice) {
        this.channels = channels;
        this.messages = messages;
        this.staff = staff;
        this.connectors = connectors;
        this.broker = broker;
        this.voice = voice;
    }

    public ChatChannel createChannel(String name, ChatChannel.ChannelType type, List<Long> memberIds) {
        ChatChannel channel = new ChatChannel(name, type);
        for (Long id : memberIds) {
            staff.findById(id).ifPresent(channel.getMembers()::add);
        }
        return channels.save(channel);
    }

    public List<ChatMessage> history(Long channelId) {
        return messages.findByChannelIdOrderBySentAtAsc(channelId);
    }

    public List<ChatChannel> listChannels() {
        return channels.findAll();
    }

    /**
     * Post a message from {@code senderId} into {@code channelId}, then have all
     * AI members reply. Returns the full list of messages produced (the original
     * plus any AI replies), so a caller/admin sees the whole exchange.
     */
    @Transactional
    public List<ChatMessage> postMessage(Long channelId, Long senderId, String content) {
        ChatChannel channel = channels.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("No such channel: " + channelId));
        Staff sender = staff.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("No such staff: " + senderId));

        List<ChatMessage> produced = new ArrayList<>();

        ChatMessage msg = new ChatMessage();
        msg.setChannel(channel);
        msg.setSender(sender);
        msg.setContent(content);
        ChatMessage savedMsg = messages.save(msg);
        produced.add(savedMsg);
        broadcast(channelId, savedMsg);

        // Every AI staff member in the channel (except the sender) replies.
        for (Staff member : channel.getMembers()) {
            if (member.getType() != StaffType.AI) continue;
            if (member.getId().equals(senderId)) continue;
            if (member.getStatus() != StaffStatus.ACTIVE) continue;

            AiConnector connector = connectors.forStaff(member);
            AiConnector.AiReply reply = connector.generateReply(member, content);

            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setChannel(channel);
            aiMsg.setSender(member);
            aiMsg.setContent(reply.text());
            if (member.isVoiceEnabled()) {
                var clip = voice.speak(member, reply.text());
                if (clip != null) aiMsg.setVoiceClipUrl(clip.url());
            }
            ChatMessage savedAi = messages.save(aiMsg);
            produced.add(savedAi);
            broadcast(channelId, savedAi);
        }
        return produced;
    }

    /** Push a lightweight view of a message to the channel's WebSocket topic. */
    private void broadcast(Long channelId, ChatMessage m) {
        var payload = new java.util.HashMap<String, Object>();
        payload.put("id", m.getId());
        payload.put("content", m.getContent());
        payload.put("voiceClipUrl", m.getVoiceClipUrl());
        payload.put("sentAt", m.getSentAt() == null ? null : m.getSentAt().toString());
        if (m.getSender() != null) {
            var s = new java.util.HashMap<String, Object>();
            s.put("id", m.getSender().getId());
            s.put("fullName", m.getSender().getFullName());
            s.put("type", m.getSender().getType() == null ? null : m.getSender().getType().name());
            s.put("function", m.getSender().getFunction());
            s.put("position", m.getSender().getPosition());
            payload.put("sender", s);
        }
        broker.convertAndSend("/topic/channels/" + channelId, payload);
    }
}
