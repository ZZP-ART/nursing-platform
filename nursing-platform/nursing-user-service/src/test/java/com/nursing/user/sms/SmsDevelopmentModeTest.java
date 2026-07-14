package com.nursing.user.sms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsDevelopmentModeTest {

    @Test
    void developmentModeUsesConfiguredFixedCodeAndMockProvider() {
        FixedSmsCodeGenerator generator = new FixedSmsCodeGenerator("654321");
        SmsSendResult result = new MockSmsSender().send(new SmsSendCommand(
                "13800138000", "register", generator.generate()));

        assertThat(generator.generate()).isEqualTo("654321");
        assertThat(result.success()).isTrue();
        assertThat(result.providerRequestId()).startsWith("mock-");
        assertThat(result.providerRequestId()).doesNotContain("654321");
    }

}
