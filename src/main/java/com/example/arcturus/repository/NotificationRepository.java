package com.example.arcturus.repository;

import com.example.arcturus.model.NotificationData;
import com.example.arcturus.model.NotificationResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@Transactional
public class NotificationRepository {

    @PersistenceContext
    private EntityManager em;

    public Long save(NotificationData notification) {
        em.persist(notification);
        em.flush();
        return notification.getId();
    }

    public void update(NotificationData notificationData) {
        em.merge(notificationData);
        em.flush();
    }

    public boolean isUserBlacklisted(String target) {
        return (Boolean) em.createNativeQuery("""
            SELECT EXISTS(
                SELECT 1
                FROM NOTIFICATION_BLACKLIST
                WHERE TARGET_DEST = ?1
                AND (BLOCKED_UNTIL IS NULL OR BLOCKED_UNTIL > CURRENT_TIMESTAMP)
            )
            """)
                .setParameter(1, target)
                .getSingleResult();
    }

    public int removeExpiredBlocks() {
        return em.createNativeQuery("""
                DELETE FROM NOTIFICATION_BLACKLIST
                WHERE BLOCKED_UNTIL IS NOT NULL AND BLOCKED_UNTIL <= CURRENT_TIMESTAMP
                """).executeUpdate();
    }

    public List<NotificationResponse> getHistory() {
        List<NotificationData> entries = em.createQuery(
                "SELECT n FROM NotificationData n ORDER BY n.id DESC", 
                NotificationData.class)
                .setMaxResults(20)
                .getResultList();
        return entries.stream()
                .map(this::createNotificationResponse).toList();
    }

    public NotificationResponse getLatestEntry() {
        NotificationData latest = (NotificationData) em.createNativeQuery(
                        "SELECT * FROM NOTIFICATIONS ORDER BY ID DESC LIMIT 1", NotificationData.class)
                .getSingleResult();
        return createNotificationResponse(latest);
    }

    public NotificationResponse createNotificationResponse(Long id) {
        NotificationData notificationData = em.find(NotificationData.class, id);

        if (notificationData == null) {
            throw new EntityNotFoundException("Notification not found for id: " + id);
        }

        return new NotificationResponse(
                notificationData.getId(),
                notificationData.getNotificationType(),
                notificationData.getTargetDest(),
                notificationData.getPayloadContent(),
                notificationData.getCurrentState(),
                notificationData.getFailureCount(),
                notificationData.getDispatchCount(),
                notificationData.getIsBlocked(),
                notificationData.getLastMsgOrErr(),
                notificationData.getCreatedAt(),
                notificationData.getUpdatedAt()
        );
    }

    public NotificationResponse createNotificationResponse(NotificationData data) {
        return new NotificationResponse(
                data.getId(),
                data.getNotificationType(),
                data.getTargetDest(),
                data.getPayloadContent(),
                data.getCurrentState(),
                data.getFailureCount(),
                data.getDispatchCount(),
                data.getIsBlocked(),
                data.getLastMsgOrErr(),
                data.getCreatedAt(),
                data.getUpdatedAt()
        );
    }
}
