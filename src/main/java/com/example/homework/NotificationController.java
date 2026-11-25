package com.example.homework;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService notificationService) {
        this.service = notificationService;
    }

    @PostMapping("/send")
    public NotificationResponse sendNotification(@RequestBody NotificationRequest request) throws Exception {
        return service.processNotification(request.getType(), request.getTarget(), request.getPayload());
    }

    @PostMapping("/update")
    public void updateNotification(@RequestBody NotificationRequest request) {
        service.updateNotification(request.getType(), request.getTarget(), request.getPayload());
    }

    @GetMapping("/latest")
    public NotificationResponse getLatest() {
        return service.getLatestNotificationFromDb();
    }

    @GetMapping("/history")
    public List<NotificationResponse> getHistory() {
        return service.getHistory();
    }
}
