 package com.nursing.user;
 
 import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.nursing.common.config.ApiJsonContractConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
 
@SpringBootApplication
@Import(ApiJsonContractConfiguration.class)
@EnableDiscoveryClient
@EnableScheduling
public class UserApplication {
     public static void main(String[] args) {
         SpringApplication.run(UserApplication.class, args);
     }
 }
