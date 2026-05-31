package com.linguachat.dto;

public record EventResponse(String type, Long fromUserId, Long toUserId, Object payload) {
}
