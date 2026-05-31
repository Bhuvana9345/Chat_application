package com.linguachat.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Language {
    TAMIL("ta"),
    ENGLISH("en"),
    HINDI("hi"),
    TELUGU("te"),
    MALAYALAM("ml");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Language fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Language.valueOf(value.trim().toUpperCase());
    }
}
