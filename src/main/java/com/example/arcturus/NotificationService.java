package com.example.arcturus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    private static final Map<String, INotificationHandler> REGISTRY = Map.of(
            "EMAIL", new EmailHandler(),
            "SMS", new SmsHandler(),
            "PUSH", new PushHandler()
    );

    public NotificationResponse processNotification(String type, String target, String payload)
            throws Exception {
        validateInputs(type, target, payload);
        log.debug("Processing notification: type={}, target={}", type, target);
        
        if (!REGISTRY.containsKey(type)) {
            throw new IllegalArgumentException("Unsupported notification type: " + type);
        }
        
        REGISTRY.get(type).execute(target, payload);
        
        NotificationData data = createNewNotificationData(type, target, payload);
        repository.save(data);
        
        log.info("Successfully processed notification: type={}, target={}", type, target);
        return repository.createNotificationResponse(data);
    }

    public void updateNotification(String type, String target, String payload) {
        validateInputs(type, target, payload);
        log.debug("Updating notification: type={}, target={}", type, target);
        
        NotificationData notificationData = createNewNotificationData(type, target, payload);
        repository.update(notificationData);
        log.info("Successfully updated notification: type={}, target={}", type, target);
    }

    public List<NotificationResponse> getHistory() {
        return repository.getHistory();
    }

    public NotificationResponse getLatestNotificationFromDb() {
        return repository.getLatestEntry();
    }

    private void validateInputs(String type, String target, String payload) {
        if (type == null || type.isBlank()) {
            log.warn("Validation failed: type is blank");
            throw new IllegalArgumentException("Notification type cannot be blank");
        }
        if (target == null || target.isBlank()) {
            log.warn("Validation failed: target is blank");
            throw new IllegalArgumentException("Target cannot be blank");
        }
        if (payload == null || payload.isBlank()) {
            log.warn("Validation failed: payload is blank");
            throw new IllegalArgumentException("Payload cannot be blank");
        }
        if (payload.length() > 5000) {
            log.warn("Validation failed: payload exceeds max length");
            throw new IllegalArgumentException("Payload cannot exceed 5000 characters");
        }
    }

    private NotificationData createNewNotificationData(String type, String target, String payload) {
        NotificationData notifData = new NotificationData();
        notifData.setNotificationType(type);
        notifData.setTargetDest(target);
        notifData.setPayloadContent(payload);
        notifData.setCurrentState(NotificationStatus.NEW.getCode());
        notifData.setFailureCount(0);
        notifData.setDispatchCount(0);
        notifData.setCreatedAt(LocalDateTime.now());

        return notifData;
    }

    private interface INotificationHandler {
        void execute(String target, String payload) throws Exception;
    }

    private static class EmailHandler implements INotificationHandler {
        @Override
        public void execute(String target, String payload) {
            if (target != null && target.contains("error")) {
                throw new RuntimeException("SMTP Timeout");
            }
            log.info("[EMAIL] {} -> {}", target, payload);
        }
    }

    private static class SmsHandler implements INotificationHandler {
        @Override
        public void execute(String target, String payload) {
            log.info("[SMS] {} -> {}", target, payload);
        }
    }

    private static class PushHandler implements INotificationHandler {
        @Override
        public void execute(String target, String payload) {
            log.info("[PUSH] {} -> {}", target, payload);
        }
    }
}
