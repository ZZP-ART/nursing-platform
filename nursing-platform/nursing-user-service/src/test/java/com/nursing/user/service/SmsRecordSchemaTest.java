package com.nursing.user.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SmsRecordSchemaTest {

    @Test
    void smsRecordCodeColumnCanStoreBCryptHash() throws Exception {
        Path schema = Path.of(System.getProperty("user.dir"))
                .getParent()
                .resolve("docker-compose/mysql/init/01_user_schema.sql");

        String sql = Files.readString(schema);

        assertThat(sql)
                .containsPattern("code\\s+VARCHAR\\(128\\)\\s+NOT NULL");
    }
}
