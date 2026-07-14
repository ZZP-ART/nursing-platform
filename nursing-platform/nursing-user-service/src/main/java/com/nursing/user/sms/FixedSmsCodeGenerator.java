package com.nursing.user.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile({"dev", "test"})
public class FixedSmsCodeGenerator implements SmsCodeGenerator {

    private final String code;

    public FixedSmsCodeGenerator(@Value("${nursing.sms.mock.code:123456}") String code) {
        if (!code.matches("\\d{6}")) {
            throw new IllegalStateException("nursing.sms.mock.code must contain exactly six digits");
        }
        this.code = code;
    }

    @Override
    public String generate() {
        return code;
    }
}
