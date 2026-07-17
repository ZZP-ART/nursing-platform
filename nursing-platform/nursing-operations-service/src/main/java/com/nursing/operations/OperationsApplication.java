package com.nursing.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.nursing.common.config.ApiJsonContractConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@Import(ApiJsonContractConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients
public class OperationsApplication {
    public static void main(String[] args) { SpringApplication.run(OperationsApplication.class, args); }
}
