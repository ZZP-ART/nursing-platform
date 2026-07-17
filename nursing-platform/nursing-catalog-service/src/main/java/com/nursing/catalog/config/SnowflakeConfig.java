package com.nursing.catalog.config;

import com.nursing.common.util.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {
    @Bean
    public SnowflakeIdWorker snowflakeIdWorker(@Value("${nursing.snowflake.worker-id:4}") long workerId,
                                               @Value("${nursing.snowflake.datacenter-id:1}") long datacenterId) {
        return new SnowflakeIdWorker(workerId, datacenterId);
    }
}
