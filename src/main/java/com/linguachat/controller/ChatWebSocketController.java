package com.linguachat.controller;

import com.linguachat.dto.CallSignalRequest;
import com.linguachat.dto.EventResponse;
import com.linguachat.dto.MessageRequest;
import com.linguachat.dto.ReadReceiptRequest;
import com.linguachat.dto.ReactionRequest;
import com.linguachat.dto.TypingRequest;
import com.linguachat.entity.User;
import com.linguachat.service.MessageService;
import com.linguachat.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private final MessageService messageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   UserService userService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(Principal principal, @Valid MessageRequest request) {
        messageService.send(principal.getName(), request);
    }

    @MessageMapping("/chat.reaction")
    public void reaction(Principal principal, @Valid ReactionRequest request) {
        messageService.react(principal.getName(), request);
    }

    @MessageMapping("/chat.typing")
    public void typing(Principal principal, @Valid TypingRequest request) {
        messageService.typing(principal.getName(), request.receiverId(), request.typing());
    }

    @MessageMapping("/chat.delivered")
    public void delivered(Principal principal, @Valid ReadReceiptRequest request) {
        messageService.delivered(principal.getName(), request.otherUserId());
    }

    @MessageMapping("/chat.seen")
    public void seen(Principal principal, @Valid ReadReceiptRequest request) {
        messageService.seen(principal.getName(), request.otherUserId());
    }

    @MessageMapping("/call.signal")
    public void callSignal(Principal principal, @Valid CallSignalRequest request) {
        User sender = userService.getByUsername(principal.getName());
        User receiver = userService.getById(request.receiverId());
        messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/events",
                new EventResponse("CALL_SIGNAL", sender.getId(), receiver.getId(), request));
    }
}
