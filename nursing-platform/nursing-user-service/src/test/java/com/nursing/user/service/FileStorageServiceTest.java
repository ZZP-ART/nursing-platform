package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.user.config.FileStorageProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.response.FileUploadResponse;
import com.nursing.user.exception.UserBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadAvatarImageReturnsPublicUrlAndStoresFile() {
        FileStorageService service = new FileStorageService(properties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3});

        FileUploadResponse response = service.upload(file, "avatar");

        assertThat(response.getBizType()).isEqualTo("avatar");
        assertThat(response.getFileSize()).isEqualTo(3L);
        assertThat(response.getFileName()).endsWith(".jpg");
        assertThat(response.getFileUrl()).startsWith("https://cdn.test/");
        assertThat(Files.exists(tempDir.resolve(response.getFileUrl().replace("https://cdn.test/", "")))).isTrue();
    }

    @Test
    void uploadRejectsUnsupportedBizType() {
        FileStorageService service = new FileStorageService(properties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{1});

        assertThatThrownBy(() -> service.upload(file, "other"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PARAM_ERROR));
    }

    @Test
    void uploadRejectsUnsupportedFileType() {
        FileStorageService service = new FileStorageService(properties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[]{1});

        assertThatThrownBy(() -> service.upload(file, "avatar"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ApiCode.PARAM_ERROR));
    }

    @Test
    void uploadRejectsOversizedFile() {
        FileStorageService service = new FileStorageService(properties());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "video.mp4",
                "video/mp4",
                new byte[(10 * 1024 * 1024) + 1]);

        assertThatThrownBy(() -> service.upload(file, "review_image"))
                .isInstanceOfSatisfying(UserBusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(UserErrorCode.FILE_TOO_LARGE));
    }

    private FileStorageProperties properties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.test");
        return properties;
    }
}
