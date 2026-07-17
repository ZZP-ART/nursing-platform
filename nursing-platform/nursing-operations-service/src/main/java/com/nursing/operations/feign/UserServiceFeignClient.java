package com.nursing.operations.feign;

import com.nursing.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="nursing-user-service")
public interface UserServiceFeignClient {
 @PostMapping("/internal/v1/users/{id}/roles/{role}") Result<Void> grantRole(@PathVariable("id") Long userId, @PathVariable("role") String role, @RequestHeader("X-Internal-Token") String token);
}
