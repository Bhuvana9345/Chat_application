package com.linguachat.dto;

import jakarta.validation.constraints.NotNull;

public record ReadReceiptRequest(@NotNull Long otherUserId) {
}
