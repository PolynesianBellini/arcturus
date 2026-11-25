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

    // TODO IMPLEMENT NOTIFICATION PROCESSING
    public NotificationResponse processNotification(String type, String target, String payload)
            throws Exception {

        return null;
    }

    public List<NotificationResponse> getHistory() {
        return repository.getHistory();
    }

    public NotificationResponse getLatestNotificationFromDb() {
        return repository.getLatestEntry();
    }

    public void updateNotification(String type, String target, String payload) {
        if (payload.isBlank() || target.isBlank()) {
            throw new IllegalArgumentException("Payload or target was empty");
        }
        NotificationData notificationData = createNewNotificationData(type, target, payload);
        repository.update(notificationData);
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
