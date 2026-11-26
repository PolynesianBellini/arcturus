package com.example.arcturus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "NOTIFICATIONS", schema = "PUBLIC")
public class NotificationData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_type")
    String notificationType;

    @Column(name = "target_dest")
    String targetDest;

    @Lob
    @Column(name = "payload_content")
    String payloadContent;

    @Column(name = "current_state", length = 1)
    String currentState;

    @Column(name = "failure_count")
    Integer failureCount;

    @Column(name = "dispatch_count")
    Integer dispatchCount;

    @Column(name = "is_blocked")
    Boolean isBlocked;

    @Column(name = "last_msg_or_err")
    String lastMsgOrErr;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
