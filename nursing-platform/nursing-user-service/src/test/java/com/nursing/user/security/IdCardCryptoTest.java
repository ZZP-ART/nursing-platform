package com.nursing.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class IdCardCryptoTest {
    @Test
    void encryptsAndDecryptsIdCardWithoutPlainTextLeakage() {
        IdCardCrypto crypto = new IdCardCrypto("0123456789abcdef0123456789abcdef", new MockEnvironment());
        String idCard = "11010519491231002X";

        String encrypted = crypto.encrypt(idCard);

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain(idCard);
        assertThat(crypto.decryptIfNeeded(encrypted)).isEqualTo(idCard);
    }
}
