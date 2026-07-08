package com.nursing.user.config;

import com.nursing.common.util.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        SmsProperties.class,
        FileStorageProperties.class
})
public class UserServiceConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SnowflakeIdWorker snowflakeIdWorker(
            @Value("${nursing.snowflake.worker-id:1}") long workerId,
            @Value("${nursing.snowflake.datacenter-id:1}") long datacenterId) {
        return new SnowflakeIdWorker(workerId, datacenterId);
    }
}
