package com.nursing.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class SmsCodeDigest {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] CONTEXT = "nursing:sms-code:v1".getBytes(StandardCharsets.UTF_8);
    private final byte[] key;

    public SmsCodeDigest(@Value("${nursing.jwt.secret}") String jwtSecret) {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("NURSING_JWT_SECRET is required for SMS code protection");
        }
        this.key = hmac(jwtSecret.getBytes(StandardCharsets.UTF_8), CONTEXT);
    }

    public String digest(String code) {
        return HexFormat.of().formatHex(hmac(key, code.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac(byte[] keyMaterial, byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyMaterial, HMAC_ALGORITHM));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate SMS code digest", exception);
        }
    }
}
