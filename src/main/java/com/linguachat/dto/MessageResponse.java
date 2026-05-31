package com.linguachat.dto;

import com.linguachat.entity.Language;
import com.linguachat.entity.MessageStatus;
import com.linguachat.entity.MessageType;
import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String originalText,
        String translatedText,
        MessageType messageType,
        String attachmentName,
        String attachmentData,
        Long replyToId,
        String replyPreview,
        Language sourceLanguage,
        Language targetLanguage,
        MessageStatus status,
        boolean edited,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReactionSummary> reactions
) {
}
