package com.nursing.user.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.user.config.SmsProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Component
public class AliyunSmsSender implements SmsSender {

    private static final String SUCCESS_CODE = "OK";

    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;
    private final Client client;

    public AliyunSmsSender(SmsProperties smsProperties, ObjectMapper objectMapper) throws Exception {
        this.smsProperties = smsProperties;
        this.objectMapper = objectMapper;
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        requireText(smsProperties.getSignName(), "ALIYUN_SMS_SIGN_NAME");
        requireText(aliyun.getAccessKeyId(), "ALIYUN_SMS_ACCESS_KEY_ID");
        requireText(aliyun.getAccessKeySecret(), "ALIYUN_SMS_ACCESS_KEY_SECRET");

        Config config = new Config()
                .setAccessKeyId(aliyun.getAccessKeyId())
                .setAccessKeySecret(aliyun.getAccessKeySecret());
        config.endpoint = aliyun.getEndpoint();
        this.client = new Client(config);
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        try {
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            runtimeOptions.connectTimeout = smsProperties.getAliyun().getConnectTimeoutMillis();
            runtimeOptions.readTimeout = smsProperties.getAliyun().getReadTimeoutMillis();

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(command.phone())
                    .setSignName(command.signName())
                    .setTemplateCode(command.templateCode())
                    .setTemplateParam(templateParam(command.code()));

            SendSmsResponse response = client.sendSmsWithOptions(request, runtimeOptions);
            SendSmsResponseBody body = response.getBody();
            if (body != null && SUCCESS_CODE.equals(body.getCode())) {
                String providerRequestId = StringUtils.hasText(body.getBizId()) ? body.getBizId() : body.getRequestId();
                return SmsSendResult.success(providerRequestId);
            }
            return SmsSendResult.failure(
                    body == null ? "ALIYUN_EMPTY_RESPONSE" : body.getCode(),
                    body == null ? "Aliyun SMS response body is empty" : body.getMessage());
        } catch (Exception e) {
            if (isUnknownResultException(e)) {
                return SmsSendResult.unknown("ALIYUN_SMS_UNKNOWN_RESULT", e.getMessage());
            }
            return SmsSendResult.failure("ALIYUN_SMS_EXCEPTION", e.getMessage());
        }
    }

    private boolean isUnknownResultException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) {
                return true;
            }
            String className = current.getClass().getSimpleName().toLowerCase();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (className.contains("timeout") || message.contains("timeout") || message.contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String templateParam(String code) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of("code", code));
    }

    private void requireText(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required Aliyun SMS configuration: " + envName);
        }
    }
}
