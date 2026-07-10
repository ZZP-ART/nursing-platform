package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.user.config.FileStorageProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.response.FileUploadResponse;
import com.nursing.user.entity.FileUploadRecord;
import com.nursing.user.entity.IdempotentRecord;
import com.nursing.user.exception.UserBusinessException;
import com.nursing.user.mapper.FileUploadRecordMapper;
import com.nursing.user.mapper.IdempotentRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private static final Long USER_ID = 101L;

    @TempDir
    Path tempDir;

    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;
    @Mock
    private IdempotentRecordMapper idempotentRecordMapper;
    @Mock
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Test
    void uploadAvatarImageReturnsPublicUrlAndStoresFile() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = jpg(new byte[]{1, 2, 3});
        when(snowflakeIdWorker.nextId()).thenReturn(1L, 2L);
        when(fileUploadRecordMapper.selectByUserBizHash(eq(USER_ID), eq("avatar"), any(), eq("jpg"))).thenReturn(null);
        when(idempotentRecordMapper.complete("fu:101:key-1", 2L)).thenReturn(1);

        FileUploadResponse response = service.upload(file, "avatar", USER_ID, "key-1");

        assertThat(response.getBizType()).isEqualTo("avatar");
        assertThat(response.getFileSize()).isEqualTo(3L);
        assertThat(response.getFileName()).endsWith(".jpg");
        assertThat(response.getFileUrl()).startsWith("https://cdn.test/");
        assertThat(Files.exists(tempDir.resolve(response.getFileUrl().replace("https://cdn.test/", "")))).isTrue();
        assertTempDirectoryEmpty();

        ArgumentCaptor<FileUploadRecord> recordCaptor = ArgumentCaptor.forClass(FileUploadRecord.class);
        verify(fileUploadRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getIdempotentKey()).isEqualTo("fu:101:key-1");
    }

    @Test
    void uploadRequiresIdempotentKey() {
        FileStorageService service = service();

        assertThatThrownBy(() -> service.upload(jpg(new byte[]{1}), "avatar", USER_ID, " "))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PARAM_ERROR));
    }

    @Test
    void uploadSameKeyAndSameFileReturnsExistingRecord() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        FileStorageService service = service();
        when(idempotentRecordMapper.insert(any(IdempotentRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(idempotentRecordMapper.selectByKeyForUpdate("fu:101:key-1")).thenReturn(completedIdempotent(10L));
        when(fileUploadRecordMapper.selectById(10L)).thenReturn(record(10L, "fu:101:key-1", "avatar", sha256(bytes), "jpg", 3L));

        FileUploadResponse response = service.upload(jpg(bytes), "avatar", USER_ID, "key-1");

        assertThat(response.getFileUrl()).isEqualTo("https://cdn.test/avatar/101/03/existing.jpg");
        assertTempDirectoryEmpty();
    }

    @Test
    void uploadSameKeyAndDifferentFileReturnsConflict() throws Exception {
        FileStorageService service = service();
        when(idempotentRecordMapper.insert(any(IdempotentRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(idempotentRecordMapper.selectByKeyForUpdate("fu:101:key-1")).thenReturn(completedIdempotent(10L));
        when(fileUploadRecordMapper.selectById(10L)).thenReturn(record(10L, "fu:101:key-1", "avatar", sha256(new byte[]{9}), "jpg", 1L));

        assertThatThrownBy(() -> service.upload(jpg(new byte[]{1}), "avatar", USER_ID, "key-1"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(UserErrorCode.FILE_IDEMPOTENCY_CONFLICT));
        assertTempDirectoryEmpty();
    }

    @Test
    void uploadDifferentKeyAndSameFileReusesHashRecord() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        FileStorageService service = service();
        FileUploadRecord existing = record(10L, "fu:101:key-old", "avatar", sha256(bytes), "jpg", 3L);
        when(snowflakeIdWorker.nextId()).thenReturn(1L);
        when(fileUploadRecordMapper.selectByUserBizHash(USER_ID, "avatar", sha256(bytes), "jpg")).thenReturn(existing);
        when(idempotentRecordMapper.complete("fu:101:key-new", 10L)).thenReturn(1);

        FileUploadResponse response = service.upload(jpg(bytes), "avatar", USER_ID, "key-new");

        assertThat(response.getFileUrl()).isEqualTo(existing.getFileUrl());
        assertTempDirectoryEmpty();
    }

    @Test
    void uploadRejectsUnsupportedBizType() {
        FileStorageService service = service();

        assertThatThrownBy(() -> service.upload(jpg(new byte[]{1}), "other", USER_ID, "key-1"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PARAM_ERROR));
    }

    @Test
    void uploadRejectsUnsupportedFileType() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[]{1});

        assertThatThrownBy(() -> service.upload(file, "avatar", USER_ID, "key-1"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PARAM_ERROR));
    }

    @Test
    void uploadRejectsOversizedFile() {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "video.mp4",
                "video/mp4",
                new byte[(10 * 1024 * 1024) + 1]);

        assertThatThrownBy(() -> service.upload(file, "review_image", USER_ID, "key-1"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(UserErrorCode.FILE_TOO_LARGE));
    }

    private FileStorageService service() {
        return new FileStorageService(properties(), snowflakeIdWorker, idempotentRecordMapper, fileUploadRecordMapper);
    }

    private FileStorageProperties properties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.test");
        return properties;
    }

    private MockMultipartFile jpg(byte[] bytes) {
        return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes);
    }

    private IdempotentRecord completedIdempotent(Long bizId) {
        IdempotentRecord record = new IdempotentRecord();
        record.setId(1L);
        record.setIdempotentKey("fu:101:key-1");
        record.setBizType("FILE_UPLOAD");
        record.setBizId(bizId);
        record.setStatus(1);
        return record;
    }

    private FileUploadRecord record(Long id,
                                    String idempotentKey,
                                    String bizType,
                                    String fileHash,
                                    String fileExt,
                                    Long fileSize) {
        FileUploadRecord record = new FileUploadRecord();
        record.setId(id);
        record.setUserId(USER_ID);
        record.setIdempotentKey(idempotentKey);
        record.setBizType(bizType);
        record.setFileHash(fileHash);
        record.setFileExt(fileExt);
        record.setFileSize(fileSize);
        record.setFileName("existing.jpg");
        record.setFileUrl("https://cdn.test/avatar/101/03/existing.jpg");
        record.setRelativePath("avatar/101/03/existing.jpg");
        return record;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void assertTempDirectoryEmpty() throws Exception {
        Path tempUploadDir = tempDir.resolve(".tmp").resolve(String.valueOf(USER_ID));
        if (!Files.exists(tempUploadDir)) {
            return;
        }
        try (var files = Files.list(tempUploadDir)) {
            assertThat(files).isEmpty();
        }
    }
}
