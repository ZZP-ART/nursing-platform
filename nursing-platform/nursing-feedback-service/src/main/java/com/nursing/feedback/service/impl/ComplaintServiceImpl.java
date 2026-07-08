package com.nursing.feedback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.feign.OrderFeignClient;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitComplaintRequest;
import com.nursing.feedback.dto.response.ComplaintSubmitResponse;
import com.nursing.feedback.dto.response.ComplaintTrackListVO;
import com.nursing.feedback.dto.response.ComplaintTrackVO;
import com.nursing.feedback.dto.response.ComplaintVO;
import com.nursing.feedback.entity.Complaint;
import com.nursing.feedback.entity.ComplaintTrack;
import com.nursing.feedback.repository.ComplaintMapper;
import com.nursing.feedback.repository.ComplaintTrackMapper;
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
    private final OrderFeignClient orderFeignClient;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final ObjectMapper objectMapper;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
                                ComplaintTrackMapper complaintTrackMapper,
                                OrderFeignClient orderFeignClient,
                                SnowflakeIdWorker snowflakeIdWorker,
                                ObjectMapper objectMapper) {
        this.complaintMapper = complaintMapper;
        this.complaintTrackMapper = complaintTrackMapper;
        this.orderFeignClient = orderFeignClient;
        this.snowflakeIdWorker = snowflakeIdWorker;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ComplaintSubmitResponse submitComplaint(SubmitComplaintRequest request, Long userId, String idempotentKey) {
        String key = requireIdempotentKey(idempotentKey);
        Complaint existing = complaintMapper.selectByIdempotentKey(key);
        if (existing != null) {
            verifyComplaintOwner(existing, userId);
            return new ComplaintSubmitResponse(existing.getId());
        }

        requireComplaintOrder(request.getOrderId(), userId);
        LocalDateTime now = LocalDateTime.now();
        Long complaintId = snowflakeIdWorker.nextId();
        Complaint complaint = new Complaint();
        complaint.setId(complaintId);
        complaint.setOrderId(request.getOrderId());
        complaint.setUserId(userId);
        complaint.setType(request.getType());
        complaint.setContent(trimToNull(request.getContent()));
        complaint.setImages(toJson(request.getImages()));
        complaint.setStatus(COMPLAINT_STATUS_PENDING);
        complaint.setIdempotentKey(key);
        complaint.setIsDeleted(NOT_DELETED);
        complaint.setCreateTime(now);
        complaint.setUpdateTime(now);
        try {
            complaintMapper.insert(complaint);
        } catch (DuplicateKeyException ex) {
            Complaint duplicate = complaintMapper.selectByIdempotentKey(key);
            if (duplicate != null) {
                verifyComplaintOwner(duplicate, userId);
                return new ComplaintSubmitResponse(duplicate.getId());
            }
            throw ex;
        }

        ComplaintTrack track = new ComplaintTrack();
        track.setId(snowflakeIdWorker.nextId());
        track.setComplaintId(complaintId);
        track.setOperator("系统");
        track.setContent("已收到投诉，正在核实");
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
            throw new BusinessException(ApiCode.PARAM_ERROR, "complaintId不能为空");
        }
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new BusinessException(ApiCode.COMPLAINT_NOT_FOUND, "投诉不存在");
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
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotent-Key不能为空");
        }
        String key = idempotentKey.trim();
        if (key.length() > 64) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotent-Key过长");
        }
        return key;
    }

    private void requireComplaintOrder(Long orderId, Long userId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "orderId不能为空");
        }
        OrderDTO order;
        try {
            Result<OrderDTO> result = orderFeignClient.getOrder(orderId);
            if (result == null || result.getCode() != ApiCode.SUCCESS || result.getData() == null) {
                throw new BusinessException(ApiCode.NOT_FOUND, "订单不存在");
            }
            order = result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to query order for complaint: orderId={}", orderId, ex);
            throw new BusinessException(ApiCode.BIZ_ERROR, "订单服务暂不可用");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权操作该订单");
        }
        Integer status = order.getStatus();
        if (!Integer.valueOf(OrderStatus.WAITING_SERVICE.getValue()).equals(status)
                && !Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(status)) {
            throw new BusinessException(ApiCode.BIZ_ERROR, "当前订单状态不可投诉");
        }
    }

    private void verifyComplaintOwner(Complaint complaint, Long userId) {
        if (!Objects.equals(complaint.getUserId(), userId)) {
            throw new BusinessException(ApiCode.COMPLAINT_NO_PERMISSION, "无权操作该投诉");
        }
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
            return "未知";
        }
        return switch (type) {
            case 1 -> "服务质量";
            case 2 -> "服务态度";
            case 3 -> "乱收费";
            case 4 -> "其他";
            default -> "未知";
        };
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已处理";
            case 3 -> "已关闭";
            default -> "未知";
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
            throw new BusinessException(ApiCode.PARAM_ERROR, "图片地址格式错误");
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
}
