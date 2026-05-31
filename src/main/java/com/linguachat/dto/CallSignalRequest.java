package com.linguachat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CallSignalRequest(
        @NotNull Long receiverId,
        @NotBlank String signalType,
        String callType,
        String sdp,
        String candidate
) {
}
