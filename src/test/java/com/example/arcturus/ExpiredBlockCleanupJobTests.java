package com.example.arcturus;

import com.example.arcturus.job.ExpiredBlockCleanupJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ExpiredBlockCleanupJobTests {

    @Autowired
    private ExpiredBlockCleanupJob job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearBlocks() {
        jdbcTemplate.update("DELETE FROM NOTIFICATION_BLACKLIST");
    }

    @Test
    void removesExpiredBlock() {
        insertBlock("expired", -1);

        job.removeExpiredBlocks();

        assertEquals(List.of(), blockedTargets());
    }

    @Test
    void retainsUnexpiredBlock() {
        insertBlock("future", 1);

        job.removeExpiredBlocks();

        assertEquals(List.of("future"), blockedTargets());
    }

    @Test
    void removesOnlyExpiredBlocksFromMixedCollection() {
        insertBlock("expired", -1);
        insertBlock("future", 1);
        insertBlock("indefinite", null);

        job.removeExpiredBlocks();

        assertEquals(List.of("future", "indefinite"), blockedTargets());
    }

    @Test
    void movesPreviouslyBlockedNotificationsToReview() {
        insertBlock("expired", -1);
        insertBlock("future", 1);
        insertBlock("indefinite", null);
        insertNotification("expired", "R", true);
        insertNotification("expired", "R", true);
        insertNotification("expired", "N", false);
        insertNotification("future", "R", true);
        insertNotification("indefinite", "R", true);

        job.removeExpiredBlocks();

        assertEquals(2, countNotifications("expired", "I"));
        assertEquals(1, countNotifications("expired", "N"));
        assertEquals(1, countNotifications("future", "R"));
        assertEquals(1, countNotifications("indefinite", "R"));
        assertEquals(List.of("future", "indefinite"), blockedTargets());
    }

    private void insertBlock(String target, Integer daysUntilExpiry) {
        jdbcTemplate.update("""
                INSERT INTO NOTIFICATION_BLACKLIST (TARGET_DEST, BLOCKED_AT, BLOCKED_UNTIL, BLOCKING_REASON)
                VALUES (?, CURRENT_TIMESTAMP, DATEADD('DAY', ?, CURRENT_TIMESTAMP), 'test')
                """, target, daysUntilExpiry);
    }

    private List<String> blockedTargets() {
        return jdbcTemplate.queryForList(
                "SELECT TARGET_DEST FROM NOTIFICATION_BLACKLIST ORDER BY TARGET_DEST", String.class);
    }

    private void insertNotification(String target, String status, boolean blocked) {
        jdbcTemplate.update("""
                INSERT INTO NOTIFICATIONS (NOTIFICATION_TYPE, TARGET_DEST, PAYLOAD_CONTENT, CURRENT_STATE,
                    FAILURE_COUNT, DISPATCH_COUNT, IS_BLOCKED, CREATED_AT, UPDATED_AT)
                VALUES ('EMAIL', ?, 'message', ?, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, target, status, blocked);
    }

    private int countNotifications(String target, String status) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM NOTIFICATIONS WHERE TARGET_DEST = ? AND CURRENT_STATE = ?
                """, Integer.class, target, status);
        if (count == null) {
            throw new AssertionError("Notification count is null");
        }
        return count;
    }

}
