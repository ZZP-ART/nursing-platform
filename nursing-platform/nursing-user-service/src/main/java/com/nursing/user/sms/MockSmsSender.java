package com.nursing.user.sms;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Component
@Profile({"dev", "test"})
public class MockSmsSender implements SmsSender {

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        return SmsSendResult.success("mock-" + UUID.randomUUID());
    }
}
