package com.nursing.user.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class IdCardCrypto {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String configuredKey;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    public IdCardCrypto(@Value("${nursing.security.id-card-key:}") String configuredKey,
                        Environment environment) {
        this.configuredKey = configuredKey;
        this.environment = environment;
    }

    @PostConstruct
    void validateKey() {
        if (isProd() && !StringUtils.hasText(configuredKey)) {
            throw new IllegalStateException("NURSING_ID_CARD_ENCRYPTION_KEY is required in prod");
        }
        if (StringUtils.hasText(configuredKey)) {
            key();
        }
    }

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt id card", ex);
        }
    }

    public String decryptIfNeeded(String storedValue) {
        if (!StringUtils.hasText(storedValue) || !storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            String[] parts = storedValue.substring(PREFIX.length()).split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt id card", ex);
        }
    }

    private SecretKey key() {
        byte[] raw = decodeKey(configuredKey);
        if (raw.length != 32) {
            throw new IllegalStateException("Id card encryption key must be 32 bytes");
        }
        return new SecretKeySpec(raw, "AES");
    }

    private byte[] decodeKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Id card encryption key is not configured");
        }
        if (value.startsWith("base64:")) {
            return Base64.getDecoder().decode(value.substring("base64:".length()));
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private boolean isProd() {
        return environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
