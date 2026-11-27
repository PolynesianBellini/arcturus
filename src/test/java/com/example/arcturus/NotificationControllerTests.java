package com.example.arcturus;

import com.example.arcturus.controller.GlobalExceptionHandler;
import com.example.arcturus.controller.NotificationController;
import com.example.arcturus.model.NotificationData;
import com.example.arcturus.model.NotificationResponse;
import com.example.arcturus.repository.NotificationRepository;
import com.example.arcturus.service.NotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        entityManager.createQuery("DELETE FROM NotificationData").executeUpdate();
    }

    @Test
    void testGetHistoryReturns20LatestWhen21EntriesPresent() {
        insertTestNotifications(21);

        List<NotificationResponse> response = service.getHistory();

        assertEquals(20, response.size(), "Should return exactly 20 items");
        assertEquals(21L, response.getFirst().getId(), "First item should be the latest (ID 21)");
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

    @Test
    void testNotificationForBlockedTargetIsRecordedAsRejected() throws Exception {
        entityManager.createNativeQuery("""
                INSERT INTO NOTIFICATION_BLACKLIST (TARGET_DEST, BLOCKED_AT, BLOCKED_UNTIL, BLOCKING_REASON)
                VALUES ('error@example.com', CURRENT_TIMESTAMP, DATEADD('DAY', 1, CURRENT_TIMESTAMP), 'test')
                """).executeUpdate();

        NotificationResponse response = service.processNotification("EMAIL", "error@example.com", "message");

        assertEquals("R", response.getStatus());
        assertTrue(response.isBlocked());
        assertEquals(0, response.getDispatchCount());
        entityManager.clear();
        NotificationData saved = entityManager.find(NotificationData.class, response.getId());
        assertEquals("R", saved.getCurrentState());
        assertTrue(saved.getIsBlocked());
        verifyNoInteractions(mailSender);
    }

    @Test
    void testEmailIsSentAndRecorded() throws Exception {
        NotificationResponse response = service.processNotification("EMAIL", "user@example.com", "hello");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertEquals("notifications@example.com", message.getValue().getFrom());
        String[] recipients = message.getValue().getTo();
        if (recipients == null) {
            throw new AssertionError("Email has no recipients");
        }
        assertEquals("user@example.com", recipients[0]);
        assertEquals("Notification", message.getValue().getSubject());
        assertEquals("hello", message.getValue().getText());
        assertEquals("S", response.getStatus());
        assertEquals(1, response.getDispatchCount());

        entityManager.clear();
        NotificationData saved = entityManager.find(NotificationData.class, response.getId());
        assertEquals("S", saved.getCurrentState());
        assertEquals(1, saved.getDispatchCount());
    }

    @Test
    void testEmailFailureIsRecordedAndPropagated() {
        TestTransaction.end();
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        MailSendException error = assertThrows(MailSendException.class,
                () -> service.processNotification("EMAIL", "user@example.com", "hello"));

        assertEquals("SMTP unavailable", error.getMessage());
        List<NotificationResponse> history = service.getHistory();
        assertEquals(1, history.size());
        assertEquals("E", history.getFirst().getStatus());
        assertEquals(1, history.getFirst().getDispatchCount());
        assertEquals(1, history.getFirst().getFailureCount());
        assertEquals("Email delivery failed", history.getFirst().getMessage());
    }

    @Test
    void testExpiredBlockAllowsEmail() throws Exception {
        entityManager.createNativeQuery("""
                INSERT INTO NOTIFICATION_BLACKLIST (TARGET_DEST, BLOCKED_AT, BLOCKED_UNTIL, BLOCKING_REASON)
                VALUES ('user@example.com', CURRENT_TIMESTAMP, DATEADD('DAY', -1, CURRENT_TIMESTAMP), 'test')
                """).executeUpdate();

        NotificationResponse response = service.processNotification("EMAIL", "user@example.com", "hello");

        assertEquals("S", response.getStatus());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEndpointReturnsServerErrorWhenEmailFails() throws Exception {
        TestTransaction.end();
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","target":"user@example.com","payload":"hello"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));

        assertEquals("E", service.getHistory().getFirst().getStatus());
    }

    @Test
    void testUpdateChangesOnlyTheRequestedNotification() throws Exception {
        insertTestNotifications(2);
        entityManager.clear();
        List<NotificationResponse> before = service.getHistory();
        NotificationResponse original = before.getFirst();
        NotificationResponse other = before.get(1);

        MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/api/notifications/update/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SMS","target":"new-target","payload":"updated message"}
                                """))
                .andExpect(status().isOk());

        entityManager.clear();
        List<NotificationResponse> after = service.getHistory();
        assertEquals(2, after.size());
        assertEquals(original.getId(), after.getFirst().getId());
        assertEquals("SMS", after.getFirst().getType());
        assertEquals("new-target", after.getFirst().getTarget());
        assertEquals("updated message", after.getFirst().getPayload());
        assertEquals(original.getStatus(), after.getFirst().getStatus());
        assertEquals(original.getDispatchCount(), after.getFirst().getDispatchCount());
        assertEquals(original.getCreatedAt(), after.getFirst().getCreatedAt());
        assertEquals(other.getTarget(), after.get(1).getTarget());
        assertEquals(other.getPayload(), after.get(1).getPayload());
        verifyNoInteractions(mailSender);
    }

    @Test
    void testUpdateReturnsNotFoundWithoutInserting() throws Exception {
        MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/api/notifications/update/{id}", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SMS","target":"new-target","payload":"updated message"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));

        assertEquals(0, service.getHistory().size());
        verifyNoInteractions(mailSender);
    }

    @Test
    void testSentNotificationCannotBeUpdated() throws Exception {
        NotificationResponse original = service.processNotification("EMAIL", "user@example.com", "original");
        entityManager.flush();
        entityManager.clear();

        MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(post("/api/notifications/update/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SMS","target":"new-target","payload":"changed"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sent notifications cannot be updated"));

        entityManager.clear();
        NotificationResponse saved = service.getHistory().getFirst();
        assertEquals(original.getId(), saved.getId());
        assertEquals("S", saved.getStatus());
        assertEquals("EMAIL", saved.getType());
        assertEquals("user@example.com", saved.getTarget());
        assertEquals("original", saved.getPayload());
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
