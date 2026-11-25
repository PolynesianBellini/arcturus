package com.example.arcturus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class NotificationResponse {

    private long id;
    private String type;
    private String target;
    private String payload;
    private String status;
    private int failureCount;
    private int dispatchCount;
    private boolean blocked;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
