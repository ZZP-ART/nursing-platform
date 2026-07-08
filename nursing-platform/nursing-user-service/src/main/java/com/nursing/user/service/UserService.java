package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.request.LoginRequest;
import com.nursing.user.dto.request.RegisterRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.dto.response.UserInfoResponse;
import com.nursing.user.entity.User;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserService {

    private static final String LOGIN_MODE_PASSWORD = "password";
    private static final String LOGIN_MODE_SMS = "sms";
    private static final String SMS_TYPE_REGISTER = "register";
    private static final String SMS_TYPE_LOGIN = "login";

    private final UserMapper userMapper;
    private final SmsService smsService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public UserService(UserMapper userMapper,
                       SmsService smsService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       SnowflakeIdWorker snowflakeIdWorker) {
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User existing = userMapper.selectByPhone(request.getPhone());
        if (existing != null) {
            throw new UserBusinessException(
                    HttpStatus.CONFLICT,
                    UserErrorCode.REGISTER_PHONE_CONFLICT,
                    "该手机号已注册");
        }
        smsService.verifySmsCode(request.getPhone(), SMS_TYPE_REGISTER, request.getSmsCode());

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(snowflakeIdWorker.nextId());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(defaultNickname(request));
        user.setGender(0);
        user.setStatus(0);
        user.setIsDeleted(0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_NOT_REGISTERED,
                    "手机号未注册");
        }
        ensureEnabled(user);

        if (LOGIN_MODE_PASSWORD.equals(request.getLoginMode())) {
            validatePasswordLogin(request, user);
        } else if (LOGIN_MODE_SMS.equals(request.getLoginMode())) {
            validateSmsLogin(request);
        } else {
            throw paramError("登录方式不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLastLoginTime(user.getId(), now);
        user.setLastLoginTime(now);
        return issueAuthResponse(user);
    }

    public void logout(String authorizationHeader) {
        String token = tokenService.resolveBearerToken(authorizationHeader);
        tokenService.validateToken(token);
        tokenService.invalidateToken(token);
    }

    private void validatePasswordLogin(LoginRequest request, User user) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw paramError("密码不能为空");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.PASSWORD_INVALID,
                    "密码错误");
        }
    }

    private void validateSmsLogin(LoginRequest request) {
        if (!StringUtils.hasText(request.getSmsCode())) {
            throw paramError("验证码不能为空");
        }
        smsService.verifySmsCode(request.getPhone(), SMS_TYPE_LOGIN, request.getSmsCode());
    }

    private void ensureEnabled(User user) {
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.ACCOUNT_DISABLED,
                    "账号已被禁用");
        }
    }

    private AuthResponse issueAuthResponse(User user) {
        TokenService.TokenIssue tokenIssue = tokenService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setToken(tokenIssue.getToken());
        response.setExpireTime(tokenIssue.getExpireTime());
        response.setUser(toAuthUserInfo(user));
        return response;
    }

    private UserInfoResponse toAuthUserInfo(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setPhone(maskPhone(user.getPhone()));
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setGender(user.getGender());
        response.setStatus(user.getStatus());
        return response;
    }

    private String defaultNickname(RegisterRequest request) {
        if (StringUtils.hasText(request.getNickname())) {
            return request.getNickname();
        }
        String phone = request.getPhone();
        return "用户" + phone.substring(phone.length() - 4);
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private UserBusinessException paramError(String message) {
        return new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, message);
    }
}
