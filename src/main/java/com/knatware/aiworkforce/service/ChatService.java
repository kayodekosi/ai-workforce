package com.knatware.aiworkforce.service;

import com.knatware.aiworkforce.connector.AiConnector;
import com.knatware.aiworkforce.connector.AiConnectorRegistry;
import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Core chat logic. Supports direct, group, and conference channels. When a
 * message is posted, every AI staff member in the channel (other than the
 * sender) generates a reply through its configured connector — this is how AI
 * staff "talk to each other" and how the admin simulates conversations.
 */
@Service
public class ChatService {

    private final ChatChannelRepository channels;
    private final ChatMessageRepository messages;
    private final StaffRepository staff;
    private final AiConnectorRegistry connectors;

    public ChatService(ChatChannelRepository channels, ChatMessageRepository messages,
                       StaffRepository staff, AiConnectorRegistry connectors) {
        this.channels = channels;
        this.messages = messages;
        this.staff = staff;
        this.connectors = connectors;
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
        produced.add(messages.save(msg));

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
            if (member.isVoiceEnabled()) aiMsg.setVoiceClipUrl(reply.voiceClipUrl());
            produced.add(messages.save(aiMsg));
        }
        return produced;
    }
}
