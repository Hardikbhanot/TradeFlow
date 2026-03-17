package com.tradeflow.market_service.listener;

import com.tradeflow.market_service.dto.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "order-completed-topic", groupId = "market-service-group")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("🔔 Order Completed Event received for User {}: {} {}", 
                 event.getUserId(), event.getSide(), event.getSymbol());
        
        // Push notification to the specific user's topic
        String destination = "/topic/orders/" + event.getUserId();
        messagingTemplate.convertAndSend(destination, event);
        
        log.info("📡 Broadcasted order update to {}", destination);
    }
}
