package com.nursing.feedback.job;

import com.nursing.feedback.repository.IdempotentRecordMapper;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotentRecordCleanupJob {
    private static final int BATCH_SIZE = 1_000;

    private final IdempotentRecordMapper idempotentRecordMapper;

    public IdempotentRecordCleanupJob(IdempotentRecordMapper idempotentRecordMapper) {
        this.idempotentRecordMapper = idempotentRecordMapper;
    }

    @Scheduled(fixedDelayString = "${nursing.idempotency.cleanup-delay-ms:3600000}")
    public void deleteExpiredRecords() {
        idempotentRecordMapper.deleteExpiredBefore(LocalDateTime.now(), BATCH_SIZE);
    }
}
