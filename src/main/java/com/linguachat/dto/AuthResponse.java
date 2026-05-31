package com.linguachat.dto;

import com.linguachat.entity.Language;

public record AuthResponse(String token, Long id, String username, String email, Language preferredLanguage) {
}
