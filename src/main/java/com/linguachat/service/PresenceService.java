package com.linguachat.service;

import com.linguachat.dto.EventResponse;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void userOnline(String username, Long userId) {
        onlineUsers.add(username);
        messagingTemplate.convertAndSend("/topic/presence", new EventResponse("ONLINE", userId, null, true));
    }

    public void userOffline(String username, Long userId) {
        onlineUsers.remove(username);
        messagingTemplate.convertAndSend("/topic/presence", new EventResponse("OFFLINE", userId, null, false));
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }
}
