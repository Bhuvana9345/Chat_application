package com.linguachat.service;

import com.linguachat.dto.EditMessageRequest;
import com.linguachat.dto.EventResponse;
import com.linguachat.dto.MessageRequest;
import com.linguachat.dto.MessageResponse;
import com.linguachat.dto.ReactionRequest;
import com.linguachat.dto.ReactionSummary;
import com.linguachat.entity.Message;
import com.linguachat.entity.MessageStatus;
import com.linguachat.entity.MessageType;
import com.linguachat.entity.Reaction;
import com.linguachat.entity.Translation;
import com.linguachat.entity.User;
import com.linguachat.exception.AppException;
import com.linguachat.repository.MessageRepository;
import com.linguachat.repository.ReactionRepository;
import com.linguachat.repository.TranslationRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final TranslationRepository translationRepository;
    private final UserService userService;
    private final TranslationService translationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ReactionRepository reactionRepository,
                          TranslationRepository translationRepository,
                          UserService userService,
                          TranslationService translationService,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.translationRepository = translationRepository;
        this.userService = userService;
        this.translationService = translationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageResponse send(String senderUsername, MessageRequest request) {
        User sender = userService.getByUsername(senderUsername);
        User receiver = userService.getById(request.receiverId());
        String cleanText = request.text() == null ? "" : request.text().trim();
        MessageType type = request.messageType() == null ? MessageType.TEXT : request.messageType();
        if (cleanText.isBlank() && type == MessageType.TEXT) {
            throw new AppException("Message cannot be empty", HttpStatus.BAD_REQUEST);
        }
        String translated = translationService.translate(
                cleanText, sender.getPreferredLanguage(), receiver.getPreferredLanguage());

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalText(cleanText.isBlank() ? request.attachmentName() : cleanText);
        message.setTranslatedText(translated);
        message.setMessageType(type);
        message.setAttachmentName(request.attachmentName());
        message.setAttachmentData(request.attachmentData());
        message.setReplyToId(request.replyToId());
        if (request.replyToId() != null) {
            messageRepository.findById(request.replyToId())
                    .ifPresent(reply -> message.setReplyPreview(reply.isDeleted()
                            ? "Deleted message"
                            : trimPreview(reply.getOriginalText())));
        }
        message.setSourceLanguage(sender.getPreferredLanguage());
        message.setTargetLanguage(receiver.getPreferredLanguage());
        message.setStatus(MessageStatus.SENT);
        Message saved = messageRepository.save(message);

        Translation translation = new Translation();
        translation.setMessage(saved);
        translation.setOriginalText(saved.getOriginalText());
        translation.setTranslatedText(saved.getTranslatedText());
        translation.setSourceLanguage(saved.getSourceLanguage());
        translation.setTargetLanguage(saved.getTargetLanguage());
        translationRepository.save(translation);

        MessageResponse response = toResponse(saved);
        sendToUser(receiver.getUsername(), "MESSAGE", sender.getId(), receiver.getId(), response);
        sendToUser(sender.getUsername(), "MESSAGE", sender.getId(), receiver.getId(), response);
        return response;
    }

    @Transactional
    public List<MessageResponse> history(String currentUsername, Long otherUserId) {
        User current = userService.getByUsername(currentUsername);
        userService.getById(otherUserId);
        return messageRepository.findConversation(current.getId(), otherUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MessageResponse edit(String username, Long messageId, EditMessageRequest request) {
        User current = userService.getByUsername(username);
        Message message = getOwnedMessage(messageId, current.getId());
        String translated = translationService.translate(
                request.text(), message.getSourceLanguage(), message.getTargetLanguage());
        message.setOriginalText(request.text());
        message.setTranslatedText(translated);
        message.setEdited(true);
        Message saved = messageRepository.save(message);
        translationRepository.findByMessageId(saved.getId()).ifPresent(translation -> {
            translation.setOriginalText(saved.getOriginalText());
            translation.setTranslatedText(saved.getTranslatedText());
            translation.setSourceLanguage(saved.getSourceLanguage());
            translation.setTargetLanguage(saved.getTargetLanguage());
            translationRepository.save(translation);
        });
        MessageResponse response = toResponse(saved);
        broadcastMessageEvent("EDIT", response);
        return response;
    }

    @Transactional
    public MessageResponse delete(String username, Long messageId) {
        User current = userService.getByUsername(username);
        Message message = getOwnedMessage(messageId, current.getId());
        message.setDeleted(true);
        message.setOriginalText("This message was deleted");
        message.setTranslatedText("This message was deleted");
        Message saved = messageRepository.save(message);
        MessageResponse response = toResponse(saved);
        broadcastMessageEvent("DELETE", response);
        return response;
    }

    @Transactional
    public MessageResponse react(String username, ReactionRequest request) {
        User current = userService.getByUsername(username);
        Message message = messageRepository.findById(request.messageId())
                .orElseThrow(() -> new AppException("Message not found", HttpStatus.NOT_FOUND));
        if (!message.getSender().getId().equals(current.getId()) && !message.getReceiver().getId().equals(current.getId())) {
            throw new AppException("You cannot react to this message", HttpStatus.FORBIDDEN);
        }
        reactionRepository.findByMessageIdAndUserIdAndEmoji(message.getId(), current.getId(), request.emoji())
                .ifPresentOrElse(reactionRepository::delete, () -> {
                    Reaction reaction = new Reaction();
                    reaction.setMessage(message);
                    reaction.setUser(current);
                    reaction.setEmoji(request.emoji());
                    reactionRepository.save(reaction);
                });
        reactionRepository.flush();

        MessageResponse response = toResponse(message);
        broadcastMessageEvent("REACTION", response);
        return response;
    }

    @Transactional
    public void delivered(String username, Long senderId) {
        User receiver = userService.getByUsername(username);
        User sender = userService.getById(senderId);
        messageRepository.updateDeliveredForConversation(sender.getId(), receiver.getId(), MessageStatus.DELIVERED.name());
        sendToUser(sender.getUsername(), "DELIVERED", receiver.getId(), sender.getId(), receiver.getId());
    }

    @Transactional
    public void seen(String username, Long senderId) {
        User receiver = userService.getByUsername(username);
        User sender = userService.getById(senderId);
        messageRepository.markSeen(sender.getId(), receiver.getId());
        sendToUser(sender.getUsername(), "SEEN", receiver.getId(), sender.getId(), receiver.getId());
    }

    public void typing(String username, Long receiverId, boolean typing) {
        User sender = userService.getByUsername(username);
        User receiver = userService.getById(receiverId);
        sendToUser(receiver.getUsername(), "TYPING", sender.getId(), receiver.getId(), typing);
    }

    @Transactional
    public void clearConversation(String username, Long otherUserId) {
        User current = userService.getByUsername(username);
        User other = userService.getById(otherUserId);
        messageRepository.clearConversation(current.getId(), other.getId());
        sendToUser(current.getUsername(), "CHAT_CLEARED", current.getId(), other.getId(), other.getId());
        sendToUser(other.getUsername(), "CHAT_CLEARED", current.getId(), other.getId(), current.getId());
    }

    private Message getOwnedMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException("Message not found", HttpStatus.NOT_FOUND));
        if (!message.getSender().getId().equals(userId)) {
            throw new AppException("Only sender can edit or delete this message", HttpStatus.FORBIDDEN);
        }
        return message;
    }

    private void broadcastMessageEvent(String type, MessageResponse response) {
        User sender = userService.getById(response.senderId());
        User receiver = userService.getById(response.receiverId());
        sendToUser(sender.getUsername(), type, sender.getId(), receiver.getId(), response);
        sendToUser(receiver.getUsername(), type, sender.getId(), receiver.getId(), response);
    }

    private void sendToUser(String username, String type, Long fromUserId, Long toUserId, Object payload) {
        messagingTemplate.convertAndSendToUser(username, "/queue/events",
                new EventResponse(type, fromUserId, toUserId, payload));
    }

    private MessageResponse toResponse(Message message) {
        List<ReactionSummary> summaries = reactionRepository.findByMessageId(message.getId()).stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ReactionSummary(entry.getKey(), entry.getValue()))
                .toList();

        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getReceiver().getId(),
                message.getReceiver().getUsername(),
                message.getOriginalText(),
                message.getTranslatedText(),
                message.getMessageType(),
                message.getAttachmentName(),
                message.getAttachmentData(),
                message.getReplyToId(),
                message.getReplyPreview(),
                message.getSourceLanguage(),
                message.getTargetLanguage(),
                message.getStatus(),
                message.isEdited(),
                message.isDeleted(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                summaries
        );
    }

    private String trimPreview(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}
