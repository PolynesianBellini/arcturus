package com.example.homework;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {
    private String type;
    private String target;
    private String payload;
}
