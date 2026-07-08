package com.nursing.order.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.response.PrepayTokenResponse;
import com.nursing.order.entity.IdempotentRecord;
import com.nursing.order.repository.IdempotentRecordMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IdempotentService {
    public static final String BIZ_TYPE_CREATE_ORDER = "CREATE_ORDER";
    private static final int TOKEN_EXPIRE_MINUTES = 10;

    private final IdempotentRecordMapper idempotentRecordMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public IdempotentService(IdempotentRecordMapper idempotentRecordMapper, SnowflakeIdWorker snowflakeIdWorker) {
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    public PrepayTokenResponse issuePrepayToken() {
        for (int i = 0; i < 3; i++) {
            String token = "pt_" + UUID.randomUUID().toString().replace("-", "");
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES);
            IdempotentRecord record = new IdempotentRecord();
            record.setId(snowflakeIdWorker.nextId());
            record.setIdempotentKey(token);
            record.setBizType(BIZ_TYPE_CREATE_ORDER);
            record.setStatus(0);
            record.setExpireTime(expireTime);
            try {
                idempotentRecordMapper.insert(record);
                return new PrepayTokenResponse(token, expireTime);
            } catch (DuplicateKeyException ignored) {
                // UUID collision is extraordinarily unlikely; retry with a new token.
            }
        }
        throw new IllegalStateException("Failed to issue prepay token");
    }

    public IdempotentRecord selectByKeyForUpdate(String idempotentKey) {
        return idempotentRecordMapper.selectByKeyForUpdate(idempotentKey);
    }

    public int complete(String idempotentKey, Long bizId) {
        return idempotentRecordMapper.complete(idempotentKey, bizId);
    }
}
