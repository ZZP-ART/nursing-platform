package com.nursing.user.sms;

public record SmsSendResult(
        boolean success,
        boolean resultUnknown,
        String providerRequestId,
        String errorCode,
        String errorMessage) {

    public static SmsSendResult success(String providerRequestId) {
        return new SmsSendResult(true, false, providerRequestId, null, null);
    }

    public static SmsSendResult failure(String errorCode, String errorMessage) {
        return new SmsSendResult(false, false, null, errorCode, errorMessage);
    }

    public static SmsSendResult unknown(String errorCode, String errorMessage) {
        return new SmsSendResult(false, true, null, errorCode, errorMessage);
    }

    public String failureReason() {
        if (success) {
            return null;
        }
        if (errorCode == null || errorCode.isBlank()) {
            return errorMessage;
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            return errorCode;
        }
        return errorCode + ": " + errorMessage;
    }
}
