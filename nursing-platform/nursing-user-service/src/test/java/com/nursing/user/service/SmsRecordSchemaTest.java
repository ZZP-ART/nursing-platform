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
                .contains("provider_receipt")
                .contains("sms_record_status_transition")
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

    @Test
    void schemaContainsDurableSmsRequestAndOutboxTables() throws Exception {
        String schema = Files.readString(Path.of("deploy/mysql/init/01_user_schema.sql"));

        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS sms_send_request");
        assertThat(schema).contains("UNIQUE KEY uk_sms_request_idempotency");
        assertThat(schema).contains("idx_sms_request_terminal_expiry");
        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS sms_outbox_event");
        assertThat(schema).contains("UNIQUE KEY uk_sms_outbox_request");
        assertThat(schema).contains("response_snapshot");
        assertThat(schema).contains("event_type");
        assertThat(schema).contains("lease_expire_time");
        assertThat(schema).contains("next_execute_time");
        assertThat(schema).contains("retry_count");
        assertThat(schema).contains("sms_record_status_transition");
        assertThat(schema).contains("idx_sms_outbox_dispatch");
    }
}
