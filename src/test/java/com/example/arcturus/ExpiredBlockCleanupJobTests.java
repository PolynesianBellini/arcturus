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
}
