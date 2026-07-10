package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.request.SmsCodeRequest;
import com.nursing.user.dto.response.SmsCodeResponse;
import com.nursing.user.service.SmsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/sms-code")
    public Result<SmsCodeResponse> sendSmsCode(@RequestBody @Valid SmsCodeRequest request,
                                               HttpServletRequest httpRequest) {
        Result<SmsCodeResponse> result = Result.success(smsService.sendSmsCode(request, httpRequest));
        result.setMessage("验证码已发送");
        return result;
    }
}
