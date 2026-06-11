package com.knatware.aiworkforce.controller;

import com.knatware.aiworkforce.model.*;
import com.knatware.aiworkforce.service.ChatService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Chat endpoints: create direct/group/conference channels, post messages, and
 * read history. Posting a message triggers AI staff in the channel to reply,
 * which the admin can use to simulate full conversations.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) { this.chat = chat; }

    public record CreateChannelRequest(String name, ChatChannel.ChannelType type, List<Long> memberIds) {}
    public record PostMessageRequest(Long senderId, String content) {}

    @PostMapping("/channels")
    public ChatChannel createChannel(@RequestBody CreateChannelRequest req) {
        return chat.createChannel(req.name(), req.type() == null ? ChatChannel.ChannelType.DIRECT : req.type(), req.memberIds());
    }

    @PostMapping("/channels/{channelId}/messages")
    public List<ChatMessage> post(@PathVariable Long channelId, @RequestBody PostMessageRequest req) {
        return chat.postMessage(channelId, req.senderId(), req.content());
    }

    @GetMapping("/channels/{channelId}/messages")
    public List<ChatMessage> history(@PathVariable Long channelId) {
        return chat.history(channelId);
    }
}
