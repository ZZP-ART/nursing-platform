package com.nursing.user.sms;

public interface SmsSender {

    SmsSendResult send(SmsSendCommand command);
}
