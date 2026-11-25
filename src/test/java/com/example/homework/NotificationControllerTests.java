package com.example.homework;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationControllerTests {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private NotificationService service;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.createQuery("DELETE FROM NotificationData").executeUpdate();
    }

    @Test
    void testGetHistoryReturns20LatestWhen21EntriesPresent() {
        insertTestNotifications(21);

        List<NotificationResponse> response = service.getHistory();

        assertEquals(20, response.size(), "Should return exactly 20 items");
        assertEquals(21L, response.get(0).getId(), "First item should be the latest (ID 21)");
        assertEquals(2L, response.get(19).getId(), "Last item should be ID 2 (20th latest)");
    }

    @Test
    void testGetHistoryHandlesEmptyDatabaseWithEmptyList() {
        List<NotificationResponse> response = service.getHistory();

        assertEquals(0, response.size(), "Should return empty list when no notifications exist");
    }

    @Test
    void testGetLatestNotification() {
        insertTestNotifications(3);

        NotificationResponse response = service.getLatestNotificationFromDb();

        assertEquals(3, response.getId(), "should be latest entry");
    }

    private void insertTestNotifications(int count) {
        for (int i = 1; i <= count; i++) {
            NotificationData notification = new NotificationData();
            notification.setNotificationType("EMAIL");
            notification.setTargetDest("user" + i + "@example.com");
            notification.setPayloadContent("Test message " + i);
            notification.setCurrentState("N");
            notification.setFailureCount(0);
            notification.setDispatchCount(1);
            notification.setIsBlocked(false);
            notification.setLastMsgOrErr("Sent successfully");
            notification.setCreatedAt(LocalDateTime.now().minusMinutes(count - i));
            notification.setUpdatedAt(LocalDateTime.now().minusMinutes(count - i));

            repository.save(notification);
        }
        entityManager.flush();
    }
}
