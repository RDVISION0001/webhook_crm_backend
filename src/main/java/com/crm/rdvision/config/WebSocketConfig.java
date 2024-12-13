package com.crm.rdvision.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") .setAllowedOrigins("http://localhost:3000", "https://crm.rdvision.in").withSockJS();  // Correct WebSocket endpoint
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Set the prefix for application destinations (client-to-server communication)
        registry.setApplicationDestinationPrefixes("/app");

        // Enable simple broker with destinations for broadcasting and private messaging
        registry.enableSimpleBroker("/topic", "/queue");

        // Set the user destination prefix for private messages
        registry.setUserDestinationPrefix("/user");
    }
}

