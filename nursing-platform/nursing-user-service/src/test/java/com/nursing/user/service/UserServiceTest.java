package com.nursing.user.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.request.RegisterRequest;
import com.nursing.user.dto.request.UpdateUserProfileRequest;
import com.nursing.user.dto.response.ProfileUpdateResponse;
import com.nursing.user.entity.User;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.UserMapper;
import com.nursing.user.security.IdCardCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SmsService smsService;
    @Mock
    private TokenService tokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;
    @Mock
    private IdCardCrypto idCardCrypto;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userMapper,
                smsService,
                tokenService,
                passwordEncoder,
                snowflakeIdWorker,
                idCardCrypto);
    }

    @Test
    void registerRejectsExistingPhoneBeforeSmsVerification() {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByPhone(request.getPhone())).thenReturn(new User());

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOfSatisfying(UserBusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo(UserErrorCode.REGISTER_PHONE_CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("该手机号已注册");
                });

        verify(smsService, never()).verifySmsCode(any(), any(), any());
        verify(userMapper, never()).insert(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void registerConvertsDuplicatePhoneInsertToBusinessConflict() {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByPhone(request.getPhone())).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(1001L);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("Duplicate entry"));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOfSatisfying(UserBusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo(UserErrorCode.REGISTER_PHONE_CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("该手机号已注册");
                });

        verify(smsService).verifySmsCode(request.getPhone(), "register", request.getSmsCode());
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void updateProfileUsesVersionAndReturnsIncrementedVersion() {
        User current = user(1001L, "old", null, 0, null, 3);
        User updated = user(1001L, "new", null, 0, null, 4);
        UpdateUserProfileRequest request = profileRequest(3, "new");
        when(userMapper.selectById(1001L)).thenReturn(current, updated);
        when(userMapper.updateProfileByIdAndVersion(any(User.class))).thenReturn(1);

        ProfileUpdateResponse response = userService.updateProfile(1001L, request);

        assertThat(response.getNickname()).isEqualTo("new");
        assertThat(response.getVersion()).isEqualTo(4);
        ArgumentCaptor<User> updateCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateProfileByIdAndVersion(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getVersion()).isEqualTo(3);
    }

    @Test
    void updateProfileRejectsStaleVersionWithDifferentContent() {
        User current = user(1001L, "old", null, 0, null, 3);
        UpdateUserProfileRequest request = profileRequest(2, "new");
        when(userMapper.selectById(1001L)).thenReturn(current);

        assertThatThrownBy(() -> userService.updateProfile(1001L, request))
                .isInstanceOfSatisfying(UserBusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo(UserErrorCode.PROFILE_VERSION_CONFLICT);
                });

        verify(userMapper, never()).updateProfileByIdAndVersion(any());
    }

    @Test
    void updateProfileTreatsStaleVersionWithSameContentAsSafeRetry() {
        User current = user(1001L, "new", null, 0, null, 3);
        UpdateUserProfileRequest request = profileRequest(2, "new");
        when(userMapper.selectById(1001L)).thenReturn(current);

        ProfileUpdateResponse response = userService.updateProfile(1001L, request);

        assertThat(response.getNickname()).isEqualTo("new");
        assertThat(response.getVersion()).isEqualTo(3);
        verify(userMapper, never()).updateProfileByIdAndVersion(any());
    }

    @Test
    void updateProfileComparesEncryptedIdCardByPlainTextForSafeRetry() {
        String idCard = "11010519491231002X";
        User current = user(1001L, "user", null, 0, "encrypted-id-card", 3);
        UpdateUserProfileRequest request = profileRequest(2, null);
        request.setIdCard(idCard);
        when(userMapper.selectById(1001L)).thenReturn(current);
        when(idCardCrypto.decryptIfNeeded("encrypted-id-card")).thenReturn(idCard);

        ProfileUpdateResponse response = userService.updateProfile(1001L, request);

        assertThat(response.getVersion()).isEqualTo(3);
        verify(userMapper, never()).updateProfileByIdAndVersion(any());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13812345678");
        request.setSmsCode("123456");
        request.setPassword("abc12345");
        return request;
    }

    private UpdateUserProfileRequest profileRequest(Integer version, String nickname) {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setVersion(version);
        request.setNickname(nickname);
        return request;
    }

    private User user(Long id, String nickname, String avatar, Integer gender, String idCard, Integer version) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setGender(gender);
        user.setIdCard(idCard);
        user.setStatus(0);
        user.setVersion(version);
        return user;
    }
}
