package com.linguachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(@NotNull Long messageId, @NotBlank String emoji) {
}
