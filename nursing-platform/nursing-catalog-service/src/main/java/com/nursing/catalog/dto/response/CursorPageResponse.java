package com.nursing.catalog.dto.response;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> list,
        int size,
        boolean hasNext,
        String nextCursor) {
}
