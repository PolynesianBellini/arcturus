package com.example.arcturus.model;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    NEW("N"),
    SENT("S"),
    ERROR("E"),
    UNKNOWN("U"),
    IN_REVIEW("I"),
    REJECTED("R");

    private final String code;

    NotificationStatus(String code) {
        this.code = code;
    }

}
