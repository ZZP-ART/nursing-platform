package com.nursing.operations.dto.response;

import java.time.LocalDateTime;

public record ServiceActionResponse(String action, String remark, LocalDateTime createTime) { }
