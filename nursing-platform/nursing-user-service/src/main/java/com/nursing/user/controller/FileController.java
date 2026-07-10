package com.nursing.user.controller;

import com.nursing.common.result.Result;
import com.nursing.user.dto.response.FileUploadResponse;
import com.nursing.user.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestAttribute("userId") Long userId,
                                             @RequestHeader("Idempotent-Key") String idempotentKey,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam("bizType") String bizType) {
        Result<FileUploadResponse> result = Result.success(fileStorageService.upload(file, bizType, userId, idempotentKey));
        result.setMessage("上传成功");
        return result;
    }
}
