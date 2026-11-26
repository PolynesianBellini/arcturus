package com.example.arcturus.job;

import com.example.arcturus.repository.NotificationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpiredBlockCleanupJob {

    private final NotificationRepository repository;

    public ExpiredBlockCleanupJob(NotificationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void removeExpiredBlocks() {
        int removed = repository.removeExpiredBlocks();
        log.info("Removed {} expired target blocks", removed);
    }
}
