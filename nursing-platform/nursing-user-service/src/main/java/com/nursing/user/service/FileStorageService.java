package com.nursing.user.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.user.config.FileStorageProperties;
import com.nursing.user.constant.UserErrorCode;
import com.nursing.user.dto.response.FileUploadResponse;
import com.nursing.user.exception.UserBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("avatar", "review_image", "complaint_image");
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "mp4", "video/mp4");
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageProperties fileStorageProperties;

    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public FileUploadResponse upload(MultipartFile file, String bizType) {
        validateBizType(bizType);
        validateFile(file);

        String extension = extension(file.getOriginalFilename());
        String generatedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        Path uploadRoot = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path targetDirectory = uploadRoot.resolve(datePath).normalize();
        Path target = targetDirectory.resolve(generatedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw uploadFailed();
        }

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(target.toFile());
        } catch (IOException exception) {
            throw uploadFailed();
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(generatedName);
        response.setFileSize(file.getSize());
        response.setBizType(bizType);
        response.setFileUrl(publicUrl(datePath + "/" + generatedName));
        return response;
    }

    private void validateBizType(String bizType) {
        if (!StringUtils.hasText(bizType) || !ALLOWED_BIZ_TYPES.contains(bizType)) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, "业务类型不正确");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw uploadFailed();
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new UserBusinessException(
                    HttpStatus.BAD_REQUEST,
                    UserErrorCode.FILE_TOO_LARGE,
                    "文件大小超过限制");
        }
        String extension = extension(file.getOriginalFilename());
        String expectedContentType = ALLOWED_EXTENSIONS.get(extension);
        String contentType = file.getContentType();
        if (expectedContentType == null || (StringUtils.hasText(contentType) && !expectedContentType.equalsIgnoreCase(contentType))) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, "文件格式不支持");
        }
    }

    private String extension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String publicUrl(String relativePath) {
        String base = fileStorageProperties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            return base + relativePath;
        }
        return base + "/" + relativePath;
    }

    private UserBusinessException uploadFailed() {
        return new UserBusinessException(HttpStatus.BAD_REQUEST, UserErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
    }
}
