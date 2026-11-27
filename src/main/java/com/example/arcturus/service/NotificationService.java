package com.example.arcturus.service;

import com.example.arcturus.model.NotificationData;
import com.example.arcturus.model.NotificationResponse;
import com.example.arcturus.model.NotificationStatus;
import com.example.arcturus.repository.NotificationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;
    private final Map<String, INotificationHandler> registry;

    public NotificationService(NotificationRepository repository, JavaMailSender mailSender,
                               @Value("${arcturus.mail.from}") String sender) {
        this.repository = repository;
        this.registry = Map.of(
                "EMAIL", new EmailHandler(mailSender, sender),
                "SMS", new SmsHandler(),
                "PUSH", new PushHandler()
        );
    }

    @Transactional(noRollbackFor = MailException.class)
    public NotificationResponse processNotification(String type, String target, String payload)
            throws Exception {
        validateInputs(type, target, payload);
        log.debug("Processing notification: type={}, target={}", type, target);
        
        if (!registry.containsKey(type)) {
            throw new IllegalArgumentException("Unsupported notification type: " + type);
        }

        if (repository.isUserBlacklisted(target)) {
            NotificationData data = createNewNotificationData(type, target, payload);
            data.setCurrentState(NotificationStatus.REJECTED.getCode());
            data.setIsBlocked(true);
            data.setLastMsgOrErr("Target is blocked");
            repository.save(data);
            log.warn("Notification rejected for blocked target: {}", target);
            return repository.createNotificationResponse(data);
        }

        NotificationData data = createNewNotificationData(type, target, payload);
        try {
            registry.get(type).execute(target, payload);
        } catch (MailException ex) {
            data.setCurrentState(NotificationStatus.ERROR.getCode());
            data.setDispatchCount(1);
            data.setFailureCount(1);
            data.setLastMsgOrErr("Email delivery failed");
            repository.save(data);
            throw ex;
        }

        if ("EMAIL".equals(type)) {
            data.setCurrentState(NotificationStatus.SENT.getCode());
            data.setDispatchCount(1);
        }
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
        notifData.setIsBlocked(false);
        LocalDateTime now = LocalDateTime.now();
        notifData.setCreatedAt(now);
        notifData.setUpdatedAt(now);

        return notifData;
    }

    private interface INotificationHandler {
        void execute(String target, String payload) throws Exception;
    }

    private record EmailHandler(JavaMailSender mailSender, String sender) implements INotificationHandler {

        @Override
            public void execute(String target, String payload) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(sender);
                message.setTo(target);
                message.setSubject("Notification");
                message.setText(payload);
                mailSender.send(message);
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
