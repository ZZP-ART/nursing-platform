package com.nursing.user.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsCodeDigestTest {

    @Test
    void producesStableKeyedDigestWithoutExposingTheCode() {
        SmsCodeDigest digest = new SmsCodeDigest("test-jwt-secret-with-at-least-32-bytes");

        String first = digest.digest("123456");

        assertThat(first).matches("^[0-9a-f]{64}$");
        assertThat(first).isEqualTo(digest.digest("123456"));
        assertThat(first).isNotEqualTo(digest.digest("654321"));
        assertThat(first).doesNotContain("123456");
    }
}
