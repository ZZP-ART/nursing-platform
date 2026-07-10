package com.nursing.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "nursing.sms")
public class SmsProperties {

    private String provider = "aliyun";
    private long rateLimitSeconds = 60L;
    private int phoneDailyLimit = 10;
    private int ipHourlyLimit = 60;
    private int ipDailyLimit = 300;
    private int verifyMaxAttempts = 5;
    private long expireSeconds = 300L;
    private String signName;
    private final Templates templates = new Templates();
    private final Aliyun aliyun = new Aliyun();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getRateLimitSeconds() {
        return rateLimitSeconds;
    }

    public void setRateLimitSeconds(long rateLimitSeconds) {
        this.rateLimitSeconds = rateLimitSeconds;
    }

    public int getPhoneDailyLimit() {
        return phoneDailyLimit;
    }

    public void setPhoneDailyLimit(int phoneDailyLimit) {
        this.phoneDailyLimit = phoneDailyLimit;
    }

    public int getIpHourlyLimit() {
        return ipHourlyLimit;
    }

    public void setIpHourlyLimit(int ipHourlyLimit) {
        this.ipHourlyLimit = ipHourlyLimit;
    }

    public int getIpDailyLimit() {
        return ipDailyLimit;
    }

    public void setIpDailyLimit(int ipDailyLimit) {
        this.ipDailyLimit = ipDailyLimit;
    }

    public int getVerifyMaxAttempts() {
        return verifyMaxAttempts;
    }

    public void setVerifyMaxAttempts(int verifyMaxAttempts) {
        this.verifyMaxAttempts = verifyMaxAttempts;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public Templates getTemplates() {
        return templates;
    }

    public Aliyun getAliyun() {
        return aliyun;
    }

    public String templateCodeFor(String smsType) {
        String templateCode = switch (smsType) {
            case "register" -> templates.getRegister();
            case "login" -> templates.getLogin();
            case "reset_password" -> templates.getResetPassword();
            default -> null;
        };
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalStateException("Missing SMS template for type: " + smsType);
        }
        return templateCode;
    }

    public static class Templates {
        private String register;
        private String login;
        private String resetPassword;

        public String getRegister() {
            return register;
        }

        public void setRegister(String register) {
            this.register = register;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getResetPassword() {
            return resetPassword;
        }

        public void setResetPassword(String resetPassword) {
            this.resetPassword = resetPassword;
        }
    }

    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint = "dysmsapi.aliyuncs.com";
        private int connectTimeoutMillis = 3000;
        private int readTimeoutMillis = 5000;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }
}
