package com.linguachat.controller;

import com.linguachat.dto.EditMessageRequest;
import com.linguachat.dto.MessageRequest;
import com.linguachat.dto.MessageResponse;
import com.linguachat.dto.ReactionRequest;
import com.linguachat.service.MessageService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{otherUserId}")
    public List<MessageResponse> history(Principal principal, @PathVariable Long otherUserId) {
        return messageService.history(principal.getName(), otherUserId);
    }

    @PostMapping
    public MessageResponse send(Principal principal, @Valid @RequestBody MessageRequest request) {
        return messageService.send(principal.getName(), request);
    }

    @PutMapping("/{messageId}")
    public MessageResponse edit(Principal principal,
                                @PathVariable Long messageId,
                                @Valid @RequestBody EditMessageRequest request) {
        return messageService.edit(principal.getName(), messageId, request);
    }

    @DeleteMapping("/{messageId}")
    public MessageResponse delete(Principal principal, @PathVariable Long messageId) {
        return messageService.delete(principal.getName(), messageId);
    }

    @DeleteMapping("/conversation/{otherUserId}")
    public void clearConversation(Principal principal, @PathVariable Long otherUserId) {
        messageService.clearConversation(principal.getName(), otherUserId);
    }

    @PostMapping("/reaction")
    public MessageResponse react(Principal principal, @Valid @RequestBody ReactionRequest request) {
        return messageService.react(principal.getName(), request);
    }

    @PostMapping("/delivered/{senderId}")
    public void delivered(Principal principal, @PathVariable Long senderId) {
        messageService.delivered(principal.getName(), senderId);
    }

    @PostMapping("/seen/{senderId}")
    public void seen(Principal principal, @PathVariable Long senderId) {
        messageService.seen(principal.getName(), senderId);
    }
}
