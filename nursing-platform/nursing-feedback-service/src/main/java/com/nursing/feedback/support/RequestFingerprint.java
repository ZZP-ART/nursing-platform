package com.nursing.feedback.support;

import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.common.event.OrderPaidEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.util.StringUtils;

public final class RequestFingerprint {
    private RequestFingerprint() {
    }

    public static String review(SubmitReviewRequest request) {
        return hash("review-v1", request.getOrderId(), request.getRating(), request.getContent(), request.getImages());
    }

    public static String complaint(SubmitComplaintRequest request) {
        return hash("complaint-v1", request.getOrderId(), request.getType(), request.getContent(), request.getImages());
    }

    public static String orderPaid(OrderPaidEvent event) {
        return hash("order-paid-v1", event.orderId(), event.userId(), event.serviceItemId(),
                event.orderNo(), event.paidAt());
    }

    private static String hash(Object... fields) {
        StringBuilder canonical = new StringBuilder();
        for (Object field : fields) {
            String value = canonicalValue(field);
            canonical.append(value.length()).append(':').append(value).append(';');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String canonicalValue(Object field) {
        if (field == null) {
            return "<null>";
        }
        if (field instanceof List<?> list) {
            StringBuilder value = new StringBuilder("[");
            for (Object item : list) {
                String normalized = canonicalValue(item);
                value.append(normalized.length()).append(':').append(normalized).append(';');
            }
            return value.append(']').toString();
        }
        String value = String.valueOf(field);
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
