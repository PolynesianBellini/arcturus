package com.example.arcturus.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {
    
    @NotBlank(message = "Notification type is required")
    @Pattern(regexp = "^(EMAIL|SMS|PUSH)$", message = "Type must be EMAIL, SMS, or PUSH")
    private String type;
    
    @NotBlank(message = "Target is required")
    @Size(min = 3, max = 255, message = "Target must be between 3 and 255 characters")
    private String target;
    
    @NotBlank(message = "Payload is required")
    @Size(max = 5000, message = "Payload cannot exceed 5000 characters")
    private String payload;
}
