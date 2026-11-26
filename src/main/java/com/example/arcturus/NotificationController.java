package com.example.arcturus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService notificationService) {
        this.service = notificationService;
    }

    @PostMapping("/send")
    public NotificationResponse sendNotification(@RequestBody @jakarta.validation.Valid NotificationRequest request) throws Exception {
        log.info("Received notification request: type={}, target={}", request.getType(), request.getTarget());
        try {
            NotificationResponse response = service.processNotification(request.getType(), request.getTarget(), request.getPayload());
            log.info("Notification processed successfully: id={}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("Failed to process notification: type={}, target={}", request.getType(), request.getTarget(), e);
            throw e;
        }
    }

    @PostMapping("/update")
    public void updateNotification(@RequestBody @jakarta.validation.Valid NotificationRequest request) {
        log.info("Updating notification: type={}, target={}", request.getType(), request.getTarget());
        service.updateNotification(request.getType(), request.getTarget(), request.getPayload());
    }

    @GetMapping("/latest")
    public NotificationResponse getLatest() {
        NotificationResponse latest = service.getLatestNotificationFromDb();
        log.info("Retrieved latest notification: id={}", latest.getId());
        return latest;
    }

    @GetMapping("/history")
    public List<NotificationResponse> getHistory() {
        List<NotificationResponse> history = service.getHistory();
        log.info("Retrieved {} notifications from history", history.size());
        return history;
    }
}
