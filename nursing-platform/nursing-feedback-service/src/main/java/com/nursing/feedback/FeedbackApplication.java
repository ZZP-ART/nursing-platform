 package com.nursing.feedback;
 
 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.nursing.common.config.ApiJsonContractConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.nursing.feedback.feign.OrderFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import(ApiJsonContractConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients(clients = OrderFeignClient.class)
@EnableScheduling
public class FeedbackApplication {
     public static void main(String[] args) {
         SpringApplication.run(FeedbackApplication.class, args);
     }
 }
