 package com.nursing.order;
 
 import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.nursing.common.config.ApiJsonContractConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.nursing.order.feign.CatalogServiceFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
 
@SpringBootApplication(scanBasePackages = "com.nursing")
@Import(ApiJsonContractConfiguration.class)
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(clients = CatalogServiceFeignClient.class)
public class OrderApplication {
     public static void main(String[] args) {
         SpringApplication.run(OrderApplication.class, args);
     }
 }
