package com.nursing.feedback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.dto.response.ComplaintSubmitResponse;
import com.nursing.feedback.dto.response.ComplaintTrackListVO;
import com.nursing.feedback.dto.response.ComplaintTrackVO;
import com.nursing.feedback.dto.response.ComplaintVO;
import com.nursing.feedback.entity.Complaint;
import com.nursing.feedback.entity.ComplaintTrack;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.ComplaintMapper;
import com.nursing.feedback.repository.ComplaintTrackMapper;
import com.nursing.feedback.support.RequestFingerprint;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ComplaintServiceImpl implements com.nursing.feedback.service.ComplaintService {
    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);
    private static final int COMPLAINT_STATUS_PENDING = 0;
    private static final int NOT_DELETED = 0;
    private static final int MAX_PAGE_SIZE = 100;

    private final ComplaintMapper complaintMapper;
    private final ComplaintTrackMapper complaintTrackMapper;
    private final OrderQueryService orderQueryService;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final ObjectMapper objectMapper;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                ComplaintTrackMapper complaintTrackMapper,
                                OrderQueryService orderQueryService,
                                SnowflakeIdWorker snowflakeIdWorker,
                                ObjectMapper objectMapper) {
        this.complaintMapper = complaintMapper;
        this.complaintTrackMapper = complaintTrackMapper;
        this.orderQueryService = orderQueryService;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintSubmitResponse submitComplaint(SubmitComplaintRequest request, Long userId, String idempotentKey) {
        String key = requireIdempotentKey(idempotentKey);
        String content = requireContent(request.getContent());
        String images = toJson(request.getImages());
        String requestHash = RequestFingerprint.complaint(request);
        Complaint existing = complaintMapper.selectByUserAndIdempotentKey(userId, key);
        if (existing != null) {
            return replayExisting(existing, requestHash);
        }

        requireComplaintOrder(request.getOrderId(), userId);
        LocalDateTime now = LocalDateTime.now();
        Long complaintId = snowflakeIdWorker.nextId();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setOrderId(request.getOrderId());
        complaint.setUserId(userId);
        complaint.setType(request.getType());
        complaint.setContent(content);
        complaint.setImages(images);
        complaint.setStatus(COMPLAINT_STATUS_PENDING);
        complaint.setIdempotentKey(key);
        complaint.setRequestHash(requestHash);
        complaint.setIsDeleted(NOT_DELETED);
        complaint.setCreateTime(now);
        complaint.setUpdateTime(now);
        try {
            complaintMapper.insert(complaint);
        } catch (DuplicateKeyException ex) {
            Complaint duplicate = complaintMapper.selectByUserAndIdempotentKey(userId, key);
            if (duplicate != null) {
                return replayExisting(duplicate, requestHash);
            }
            throw ex;
        }

        ComplaintTrack track = new ComplaintTrack();
        track.setId(snowflakeIdWorker.nextId());
        track.setComplaintId(complaintId);
        track.setOperator("system");
        track.setContent("Complaint received");
        track.setIsDeleted(NOT_DELETED);
        track.setCreateTime(now);
        track.setUpdateTime(now);
        complaintTrackMapper.insert(track);
        return new ComplaintSubmitResponse(complaintId);
    }

    @Override
    public PageResult<ComplaintVO> pageComplaints(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<Complaint> complaints = complaintMapper.selectPageByUserId(userId, offset, safeSize);
        long total = complaintMapper.countByUserId(userId);
        List<ComplaintVO> list = complaints.stream()
                .map(this::toComplaintVO)
                .collect(Collectors.toList());
        return PageResult.of(list, total, safePage, safeSize);
    }

    @Override
    public ComplaintTrackListVO getComplaintTracks(Long complaintId, Long userId) {
        if (complaintId == null || complaintId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "complaintId is required");
        }
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new BusinessException(ApiCode.COMPLAINT_NOT_FOUND, "Complaint not found");
        }
        verifyComplaintOwner(complaint, userId);
        List<ComplaintTrackVO> tracks = complaintTrackMapper.selectByComplaintId(complaintId).stream()
                .map(this::toTrackVO)
                .collect(Collectors.toList());
        ComplaintTrackListVO vo = new ComplaintTrackListVO();
        vo.setComplaintId(complaint.getId());
        vo.setStatus(complaint.getStatus());
        vo.setStatusText(statusText(complaint.getStatus()));
        vo.setTracks(tracks);
        return vo;
    }

    private String requireIdempotentKey(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotency-Key is required");
        }
        String key = idempotentKey.trim();
        if (key.length() > 64) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotency-Key is too long");
        }
        return key;
    }

    private void requireComplaintOrder(Long orderId, Long userId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "orderId is required");
        }
        OrderDTO order = orderQueryService.getOrder(orderId);
        if (order == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Order not found");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "No permission for this order");
        }
        Integer status = order.getStatus();
        if (!isComplaintAllowedStatus(status)) {
            throw new BusinessException(ApiCode.BIZ_ERROR, "Order status cannot be complained");
        }
    }

    private boolean isComplaintAllowedStatus(Integer status) {
        return Integer.valueOf(OrderStatus.PENDING_DISPATCH.getValue()).equals(status)
                || Integer.valueOf(OrderStatus.ASSIGNED.getValue()).equals(status)
                || Integer.valueOf(OrderStatus.ACCEPTED.getValue()).equals(status)
                || Integer.valueOf(OrderStatus.IN_SERVICE.getValue()).equals(status)
                || Integer.valueOf(OrderStatus.PENDING_CUSTOMER_CONFIRMATION.getValue()).equals(status)
                || Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(status);
    }

    private void verifyComplaintOwner(Complaint complaint, Long userId) {
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw new BusinessException(ApiCode.COMPLAINT_NO_PERMISSION, "No permission for this complaint");
        }
    }

    private ComplaintSubmitResponse replayExisting(Complaint complaint, String requestHash) {
        if (!Objects.equals(complaint.getRequestHash(), requestHash)) {
            throw new BusinessException(ApiCode.CONFLICT, "Idempotency-Key was reused with a different request");
        }
        return new ComplaintSubmitResponse(complaint.getId());
    }

    private ComplaintVO toComplaintVO(Complaint complaint) {
        ComplaintVO vo = new ComplaintVO();
        vo.setComplaintId(complaint.getId());
        vo.setOrderId(complaint.getOrderId());
        vo.setType(complaint.getType());
        vo.setTypeText(typeText(complaint.getType()));
        vo.setContent(complaint.getContent());
        vo.setImages(fromJson(complaint.getImages()));
        vo.setStatus(complaint.getStatus());
        vo.setStatusText(statusText(complaint.getStatus()));
        vo.setCreateTime(complaint.getCreateTime());
        return vo;
    }

    private ComplaintTrackVO toTrackVO(ComplaintTrack track) {
        ComplaintTrackVO vo = new ComplaintTrackVO();
        vo.setTrackId(track.getId());
        vo.setOperator(track.getOperator());
        vo.setContent(track.getContent());
        vo.setCreateTime(track.getCreateTime());
        return vo;
    }

    private String typeText(Integer type) {
        if (type == null) {
            return "unknown";
        }
        return switch (type) {
            case 1 -> "service_quality";
            case 2 -> "service_attitude";
            case 3 -> "overcharging";
            case 4 -> "other";
            default -> "unknown";
        };
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case 0 -> "pending";
            case 1 -> "processing";
            case 2 -> "resolved";
            case 3 -> "closed";
            default -> "unknown";
        };
    }

    private String toJson(List<String> images) {
        if (CollectionUtils.isEmpty(images)) {
            return null;
        }
        List<String> cleaned = images.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Invalid image urls");
        }
    }

    private List<String> fromJson(String images) {
        if (!StringUtils.hasText(images)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse complaint images json");
            return Collections.emptyList();
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String requireContent(String content) {
        String normalized = trimToNull(content);
        if (normalized == null) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Complaint content is required");
        }
        return normalized;
    }
}
