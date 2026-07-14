package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.request.SmsCodeRequest;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.service.SmsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/users")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/sms-code")
    public Result<SmsCodeResponse> sendSmsCode(@RequestBody @Valid SmsCodeRequest request,
                                                HttpServletRequest httpRequest,
                                                @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Result<SmsCodeResponse> result = Result.success(smsService.sendSmsCode(request, httpRequest, idempotencyKey));
        result.setMessage("验证码发送任务已受理");
        return result;
    }

    @GetMapping("/sms-code/requests/{requestId}")
    public Result<SmsCodeResponse> querySmsRequest(@PathVariable Long requestId,
                                                    @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return Result.success(smsService.querySmsRequest(requestId, idempotencyKey));
    }
}
