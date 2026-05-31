package com.linguachat.dto;

import com.linguachat.entity.MessageType;
import jakarta.validation.constraints.NotNull;

public record MessageRequest(
        @NotNull Long receiverId,
        String text,
        MessageType messageType,
        String attachmentName,
        String attachmentData,
        Long replyToId
) {
}
