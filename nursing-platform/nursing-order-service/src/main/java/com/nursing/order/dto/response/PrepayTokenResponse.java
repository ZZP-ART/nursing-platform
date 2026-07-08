package com.nursing.order.dto.response;

import java.time.LocalDateTime;

public record PrepayTokenResponse(String prepayToken, LocalDateTime expireTime) {
}
