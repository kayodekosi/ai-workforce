package com.knatware.aiworkforce.repository;

import com.knatware.aiworkforce.model.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {
}
