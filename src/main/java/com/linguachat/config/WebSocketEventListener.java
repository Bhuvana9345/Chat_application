package com.linguachat.config;

import com.linguachat.entity.User;
import com.linguachat.repository.UserRepository;
import com.linguachat.service.PresenceService;
import java.security.Principal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public WebSocketEventListener(UserRepository userRepository, PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            userRepository.findByUsername(principal.getName())
                    .ifPresent(user -> presenceService.userOnline(user.getUsername(), user.getId()));
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            userRepository.findByUsername(principal.getName())
                    .ifPresent(user -> presenceService.userOffline(user.getUsername(), user.getId()));
        }
    }
}
