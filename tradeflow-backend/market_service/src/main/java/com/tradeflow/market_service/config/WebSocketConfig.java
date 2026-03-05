package com.tradeflow.market_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use the /topic prefix for outgoing WebSocket communication
        config.enableSimpleBroker("/topic");
        // Use the /app prefix for others routing messages to methods annotated with
        // @MessageMapping (if needed later)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint the React frontend will connect to
        registry.addEndpoint("/ws/market").setAllowedOriginPatterns("*").withSockJS();
    }
}
