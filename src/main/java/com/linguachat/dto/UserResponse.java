package com.linguachat.dto;

import com.linguachat.entity.Language;

public record UserResponse(Long id, String username, String email, Language preferredLanguage, boolean online) {
}
