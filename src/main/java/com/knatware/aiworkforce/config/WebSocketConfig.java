package com.knatware.aiworkforce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time chat transport. Clients connect to the STOMP endpoint at /ws and
 * subscribe to /topic/channels/{id} to receive messages as they are posted
 * (including AI staff replies), instead of polling.
 *
 * Uses a simple in-memory broker — fine for a single instance. For multi-instance
 * production, swap in an external broker (RabbitMQ/ActiveMQ) — a documented step.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback for browsers/proxies that don't allow raw WebSockets
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
