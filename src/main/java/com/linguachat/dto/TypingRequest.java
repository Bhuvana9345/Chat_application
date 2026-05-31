package com.linguachat.dto;

import jakarta.validation.constraints.NotNull;

public record TypingRequest(@NotNull Long receiverId, boolean typing) {
}
