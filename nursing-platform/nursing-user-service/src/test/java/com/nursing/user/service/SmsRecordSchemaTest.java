package com.nursing.user.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SmsRecordSchemaTest {

    @Test
    void smsRecordCodeColumnCanStoreBCryptHash() throws Exception {
        String sql = Files.readString(userSchema());

        assertThat(sql)
                .containsPattern("code\\s+VARCHAR\\(128\\)\\s+NOT NULL");
    }

    @Test
    void smsRecordSchemaContainsProductionAuditColumns() throws Exception {
        String sql = Files.readString(userSchema());

        assertThat(sql)
                .contains("request_ip")
                .contains("provider_request_id")
                .contains("failure_reason")
                .contains("verify_time")
                .contains("5结果未知")
                .contains("idx_phone_type_status");
    }

    private Path userSchema() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        Path serviceDir = currentDir.getFileName().toString().equals("nursing-user-service")
                ? currentDir
                : currentDir.resolve("nursing-user-service");
        return serviceDir.resolve("deploy/mysql/init/01_user_schema.sql");
    }
}
