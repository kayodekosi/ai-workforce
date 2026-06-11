package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChannelIdOrderBySentAtAsc(Long channelId);
}
