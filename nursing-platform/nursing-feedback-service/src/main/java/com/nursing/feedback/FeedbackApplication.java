 package com.nursing.feedback;
 
 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(clients = com.nursing.common.feign.OrderFeignClient.class)
@EnableScheduling
public class FeedbackApplication {
     public static void main(String[] args) {
         SpringApplication.run(FeedbackApplication.class, args);
     }
 }
