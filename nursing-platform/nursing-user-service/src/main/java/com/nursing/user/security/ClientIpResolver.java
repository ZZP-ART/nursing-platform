package com.nursing.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ClientIpResolver {

    private static final List<String> CANDIDATE_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP");

    public String resolve(HttpServletRequest request) {
        for (String header : CANDIDATE_HEADERS) {
            String value = request.getHeader(header);
            String ip = firstValidIp(value);
            if (StringUtils.hasText(ip)) {
                return ip;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr.trim() : "unknown";
    }

    private String firstValidIp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String first = value.split(",", 2)[0].trim();
        if (!StringUtils.hasText(first) || "unknown".equalsIgnoreCase(first) || first.length() > 45) {
            return null;
        }
        return first;
    }
}
