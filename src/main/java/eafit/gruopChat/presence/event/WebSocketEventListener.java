package eafit.gruopChat.presence.event;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import eafit.gruopChat.presence.service.PresenceService;

@Component
public class WebSocketEventListener {

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Long userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId != null) presenceService.userConnected(userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Long userId = extractUserId(StompHeaderAccessor.wrap(event.getMessage()));
        if (userId != null) presenceService.userDisconnected(userId);
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        if (accessor == null) return null;
        if (accessor.getUser() == null) return null;
        String name = accessor.getUser().getName();
        if (name == null || name.isBlank()) return null;
        try {
            return Long.valueOf(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}