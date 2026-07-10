package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.request.LoginRequest;
import com.nursing.user.dto.request.RegisterRequest;
import com.nursing.user.dto.request.ResetPasswordRequest;
import com.nursing.user.dto.request.UpdateUserProfileRequest;
import com.nursing.user.dto.response.AuthResponse;
import com.nursing.user.dto.response.ProfileUpdateResponse;
import com.nursing.user.dto.response.UserInfoResponse;
import com.nursing.user.entity.User;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.UserMapper;
import com.nursing.user.security.IdCardCrypto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UserService {

    private static final String LOGIN_MODE_PASSWORD = "password";
    private static final String LOGIN_MODE_SMS = "sms";
    private static final String SMS_TYPE_REGISTER = "register";
    private static final String SMS_TYPE_LOGIN = "login";
    private static final String SMS_TYPE_RESET_PASSWORD = "reset_password";

    private final UserMapper userMapper;
    private final SmsService smsService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final IdCardCrypto idCardCrypto;

    public UserService(UserMapper userMapper,
                       SmsService smsService,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       SnowflakeIdWorker snowflakeIdWorker,
                       IdCardCrypto idCardCrypto) {
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.idCardCrypto = idCardCrypto;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User existing = userMapper.selectByPhone(request.getPhone());
        if (existing != null) {
            throw registerPhoneConflict();
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
        user.setVersion(0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw registerPhoneConflict();
        }

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
        tokenService.invalidateToken(token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.PHONE_NOT_REGISTERED,
                    "手机号未注册");
        }
        smsService.verifySmsCode(request.getPhone(), SMS_TYPE_RESET_PASSWORD, request.getSmsCode());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.PASSWORD_SAME_AS_OLD,
                    "新密码不能与旧密码相同");
        }
        userMapper.updatePassword(request.getPhone(), passwordEncoder.encode(request.getNewPassword()), LocalDateTime.now());
    }

    public UserInfoResponse getProfile(Long userId) {
        User user = requireUser(userId);
        return toProfileUserInfo(user);
    }

    @Transactional
    public ProfileUpdateResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User current = requireUser(userId);
        if (StringUtils.hasText(request.getIdCard()) && !isValidIdCard(request.getIdCard())) {
            throw new UserBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    UserErrorCode.ID_CARD_INVALID,
                    "身份证号格式不正确");
        }

        boolean sameContent = isSameProfileContent(current, request);
        if (!Objects.equals(current.getVersion(), request.getVersion())) {
            if (sameContent) {
                return toProfileUpdateResponse(current);
            }
            throw profileVersionConflict();
        }
        if (sameContent) {
            return toProfileUpdateResponse(current);
        }

        User update = new User();
        update.setId(userId);
        update.setNickname(request.getNickname());
        update.setAvatar(request.getAvatar());
        update.setGender(request.getGender());
        update.setIdCard(StringUtils.hasText(request.getIdCard()) ? idCardCrypto.encrypt(request.getIdCard()) : request.getIdCard());
        update.setVersion(request.getVersion());
        update.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateProfileByIdAndVersion(update) == 0) {
            User latest = requireUser(userId);
            if (isSameProfileContent(latest, request)) {
                return toProfileUpdateResponse(latest);
            }
            throw profileVersionConflict();
        }

        return toProfileUpdateResponse(requireUser(userId));
    }

    private boolean isSameProfileContent(User current, UpdateUserProfileRequest request) {
        if (request.getNickname() != null && !Objects.equals(request.getNickname(), current.getNickname())) {
            return false;
        }
        if (request.getAvatar() != null && !Objects.equals(request.getAvatar(), current.getAvatar())) {
            return false;
        }
        if (request.getGender() != null && !Objects.equals(request.getGender(), current.getGender())) {
            return false;
        }
        if (request.getIdCard() != null) {
            String currentIdCard = idCardCrypto.decryptIfNeeded(current.getIdCard());
            return Objects.equals(request.getIdCard(), currentIdCard);
        }
        return true;
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
        response.setVersion(user.getVersion());
        return response;
    }

    private UserInfoResponse toProfileUserInfo(User user) {
        UserInfoResponse response = toAuthUserInfo(user);
        response.setIdCard(maskIdCard(idCardCrypto.decryptIfNeeded(user.getIdCard())));
        response.setLastLoginTime(user.getLastLoginTime());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private ProfileUpdateResponse toProfileUpdateResponse(User user) {
        ProfileUpdateResponse response = new ProfileUpdateResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setGender(user.getGender());
        response.setVersion(user.getVersion());
        return response;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserBusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHORIZED, "未授权，请先登录");
        }
        ensureEnabled(user);
        return user;
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

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    private boolean isValidIdCard(String idCard) {
        if (!idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$")) {
            return false;
        }
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checks = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (idCard.charAt(i) - '0') * weights[i];
        }
        return Character.toUpperCase(idCard.charAt(17)) == checks[sum % 11];
    }

    private UserBusinessException paramError(String message) {
        return new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, message);
    }

    private UserBusinessException registerPhoneConflict() {
        return new UserBusinessException(
                HttpStatus.CONFLICT,
                UserErrorCode.REGISTER_PHONE_CONFLICT,
                "该手机号已注册");
    }

    private UserBusinessException profileVersionConflict() {
        return new UserBusinessException(
                HttpStatus.CONFLICT,
                UserErrorCode.PROFILE_VERSION_CONFLICT,
                "个人资料已被更新，请刷新后重试");
    }
}
