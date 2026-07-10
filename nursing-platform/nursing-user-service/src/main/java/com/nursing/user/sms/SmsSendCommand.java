package com.nursing.user.sms;

public record SmsSendCommand(
        String phone,
        String smsType,
        String code,
        String signName,
        String templateCode) {
}
