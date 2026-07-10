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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int IDEMPOTENT_KEY_MAX_LENGTH = 64;
    private static final int IDEMPOTENT_EXPIRE_HOURS = 24;
    private static final String BIZ_TYPE_FILE_UPLOAD = "FILE_UPLOAD";
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("avatar", "review_image", "complaint_image");
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "mp4", "video/mp4");

    private final FileStorageProperties fileStorageProperties;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final FileUploadRecordMapper fileUploadRecordMapper;

    public FileStorageService(FileStorageProperties fileStorageProperties,
                              SnowflakeIdWorker snowflakeIdWorker,
                              IdempotentRecordMapper idempotentRecordMapper,
                              FileUploadRecordMapper fileUploadRecordMapper) {
        this.fileStorageProperties = fileStorageProperties;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.fileUploadRecordMapper = fileUploadRecordMapper;
    }

    @Transactional
    public FileUploadResponse upload(MultipartFile file, String bizType, Long userId, String idempotentKey) {
        validateBizType(bizType);
        validateIdempotentKey(idempotentKey);
        validateFile(file);

        String extension = extension(file.getOriginalFilename());
        Path uploadRoot = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path tempDirectory = uploadRoot.resolve(".tmp").resolve(String.valueOf(userId)).normalize();
        if (!tempDirectory.startsWith(uploadRoot)) {
            throw uploadFailed();
        }

        StagedFile stagedFile = stageFile(file, tempDirectory);
        String scopedIdempotentKey = scopedIdempotentKey(userId, idempotentKey);
        try {
            if (!tryCreateIdempotentRecord(scopedIdempotentKey)) {
                return handleExistingIdempotent(scopedIdempotentKey, userId, bizType, stagedFile, extension);
            }

            FileUploadRecord existing = fileUploadRecordMapper.selectByUserBizHash(
                    userId, bizType, stagedFile.fileHash(), extension);
            if (existing != null) {
                completeIdempotent(scopedIdempotentKey, existing.getId());
                return toResponse(existing);
            }

            FileUploadRecord created = persistNewFile(
                    stagedFile, uploadRoot, userId, scopedIdempotentKey, bizType, extension, file);
            completeIdempotent(scopedIdempotentKey, created.getId());
            return toResponse(created);
        } finally {
            deleteIfExists(stagedFile.tempPath());
        }
    }

    private FileUploadResponse handleExistingIdempotent(String scopedIdempotentKey,
                                                        Long userId,
                                                        String bizType,
                                                        StagedFile stagedFile,
                                                        String extension) {
        IdempotentRecord record = idempotentRecordMapper.selectByKeyForUpdate(scopedIdempotentKey);
        if (record == null || !BIZ_TYPE_FILE_UPLOAD.equals(record.getBizType())) {
            throw idempotencyConflict();
        }
        if (Integer.valueOf(0).equals(record.getStatus())) {
            throw uploadProcessing();
        }
        FileUploadRecord uploadRecord = fileUploadRecordMapper.selectById(record.getBizId());
        if (uploadRecord == null) {
            throw uploadProcessing();
        }
        if (!Objects.equals(uploadRecord.getUserId(), userId)
                || !Objects.equals(uploadRecord.getBizType(), bizType)
                || !Objects.equals(uploadRecord.getFileHash(), stagedFile.fileHash())
                || !Objects.equals(uploadRecord.getFileExt(), extension)
                || !Objects.equals(uploadRecord.getFileSize(), stagedFile.fileSize())) {
            throw idempotencyConflict();
        }
        return toResponse(uploadRecord);
    }

    private boolean tryCreateIdempotentRecord(String scopedIdempotentKey) {
        LocalDateTime now = LocalDateTime.now();
        IdempotentRecord record = new IdempotentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setIdempotentKey(scopedIdempotentKey);
        record.setBizType(BIZ_TYPE_FILE_UPLOAD);
        record.setStatus(0);
        record.setExpireTime(now.plusHours(IDEMPOTENT_EXPIRE_HOURS));
        record.setCreateTime(now);
        try {
            idempotentRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private FileUploadRecord persistNewFile(StagedFile stagedFile,
                                            Path uploadRoot,
                                            Long userId,
                                            String scopedIdempotentKey,
                                            String bizType,
                                            String extension,
                                            MultipartFile file) {
        String relativePath = relativePath(userId, bizType, stagedFile.fileHash(), extension);
        Path target = uploadRoot.resolve(relativePath).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw uploadFailed();
        }

        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                deleteIfExists(stagedFile.tempPath());
            } else {
                try {
                    Files.move(stagedFile.tempPath(), target, StandardCopyOption.ATOMIC_MOVE);
                } catch (FileAlreadyExistsException ignored) {
                    deleteIfExists(stagedFile.tempPath());
                }
            }
        } catch (IOException exception) {
            throw uploadFailed();
        }

        FileUploadRecord record = new FileUploadRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setUserId(userId);
        record.setIdempotentKey(scopedIdempotentKey);
        record.setBizType(bizType);
        record.setFileHash(stagedFile.fileHash());
        record.setFileName(target.getFileName().toString());
        record.setFileUrl(publicUrl(relativePath));
        record.setRelativePath(relativePath);
        record.setFileSize(stagedFile.fileSize());
        record.setContentType(file.getContentType());
        record.setFileExt(extension);
        record.setCreateTime(LocalDateTime.now());

        try {
            fileUploadRecordMapper.insert(record);
            return record;
        } catch (DuplicateKeyException ignored) {
            FileUploadRecord existing = fileUploadRecordMapper.selectByUserBizHash(
                    userId, bizType, stagedFile.fileHash(), extension);
            if (existing != null) {
                return existing;
            }
            throw idempotencyConflict();
        }
    }

    private StagedFile stageFile(MultipartFile file, Path tempDirectory) {
        try {
            Files.createDirectories(tempDirectory);
            Path tempPath = Files.createTempFile(tempDirectory, "upload-", ".tmp");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StagedFile(tempPath, HexFormat.of().formatHex(digest.digest()), file.getSize());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw uploadFailed();
        }
    }

    private void completeIdempotent(String scopedIdempotentKey, Long uploadRecordId) {
        if (idempotentRecordMapper.complete(scopedIdempotentKey, uploadRecordId) == 0) {
            throw uploadProcessing();
        }
    }

    private void validateBizType(String bizType) {
        if (!StringUtils.hasText(bizType) || !ALLOWED_BIZ_TYPES.contains(bizType)) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, "业务类型不正确");
        }
    }

    private void validateIdempotentKey(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey) || idempotentKey.length() > IDEMPOTENT_KEY_MAX_LENGTH) {
            throw new UserBusinessException(HttpStatus.BAD_REQUEST, ApiCode.PARAM_ERROR, "Idempotent-Key不能为空或过长");
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

    private String scopedIdempotentKey(Long userId, String idempotentKey) {
        return "fu:" + userId + ":" + idempotentKey;
    }

    private String relativePath(Long userId, String bizType, String fileHash, String extension) {
        return String.join("/",
                bizType,
                String.valueOf(userId),
                fileHash.substring(0, 2),
                fileHash + "." + extension);
    }

    private FileUploadResponse toResponse(FileUploadRecord record) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileName(record.getFileName());
        response.setFileSize(record.getFileSize());
        response.setBizType(record.getBizType());
        response.setFileUrl(record.getFileUrl());
        return response;
    }

    private String publicUrl(String relativePath) {
        String base = fileStorageProperties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            return base + relativePath;
        }
        return base + "/" + relativePath;
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary upload files.
        }
    }

    private UserBusinessException uploadFailed() {
        return new UserBusinessException(HttpStatus.BAD_REQUEST, UserErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
    }

    private UserBusinessException idempotencyConflict() {
        return new UserBusinessException(HttpStatus.CONFLICT, UserErrorCode.FILE_IDEMPOTENCY_CONFLICT, "上传幂等键冲突");
    }

    private UserBusinessException uploadProcessing() {
        return new UserBusinessException(HttpStatus.CONFLICT, UserErrorCode.FILE_UPLOAD_PROCESSING, "文件上传处理中，请稍后重试");
    }

    private record StagedFile(Path tempPath, String fileHash, Long fileSize) {
    }
}
