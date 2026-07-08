package com.nursing.feedback.service.impl;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.feign.OrderFeignClient;
import com.nursing.common.result.PageResult;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.dto.response.ReviewSubmitResponse;
import com.nursing.feedback.dto.response.ReviewVO;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.entity.Review;
import com.nursing.feedback.entity.ReviewImage;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewImageMapper;
import com.nursing.feedback.repository.ReviewMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class ReviewServiceImpl implements com.nursing.feedback.service.ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final String BIZ_TYPE_REVIEW = "REVIEW";
    private static final int REVIEW_STATUS_PENDING = 1;
    private static final int NOT_DELETED = 0;
    private static final int IDEMPOTENT_PROCESSING = 0;
    private static final int IDEMPOTENT_COMPLETED = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final OrderFeignClient orderFeignClient;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             ReviewImageMapper reviewImageMapper,
                             IdempotentRecordMapper idempotentRecordMapper,
                             OrderFeignClient orderFeignClient,
                             SnowflakeIdWorker snowflakeIdWorker) {
        this.reviewMapper = reviewMapper;
        this.reviewImageMapper = reviewImageMapper;
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.orderFeignClient = orderFeignClient;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewSubmitResponse submitReview(SubmitReviewRequest request, Long userId, String idempotentKey) {
        String key = requireIdempotentKey(idempotentKey);
        IdempotentRecord existingRecord = idempotentRecordMapper.selectByKey(key);
        if (existingRecord != null) {
            if (Integer.valueOf(IDEMPOTENT_COMPLETED).equals(existingRecord.getStatus()) && existingRecord.getBizId() != null) {
                return new ReviewSubmitResponse(existingRecord.getBizId());
            }
            throw new BusinessException(ApiCode.CONFLICT, "请求正在处理中");
        }

        createProcessingRecord(key);
        OrderDTO order = requireReviewableOrder(request.getOrderId(), userId);
        Review duplicate = reviewMapper.selectByOrderId(request.getOrderId());
        if (duplicate != null) {
            throw new BusinessException(ApiCode.REVIEW_DUPLICATE, "该订单已评价");
        }

        LocalDateTime now = LocalDateTime.now();
        Long reviewId = snowflakeIdWorker.nextId();
        Review review = new Review();
        review.setId(reviewId);
        review.setOrderId(request.getOrderId());
        review.setUserId(userId);
        review.setServiceItemId(order.getServiceItemId());
        review.setRating(request.getRating());
        review.setContent(trimToNull(request.getContent()));
        review.setStatus(REVIEW_STATUS_PENDING);
        review.setIsDeleted(NOT_DELETED);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        reviewMapper.insert(review);

        List<ReviewImage> images = buildReviewImages(reviewId, request.getImages());
        if (!images.isEmpty()) {
            reviewImageMapper.insertBatch(images);
        }
        idempotentRecordMapper.updateCompleted(key, reviewId);
        return new ReviewSubmitResponse(reviewId);
    }

    @Override
    public PageResult<ReviewVO> pageReviews(Long itemId, int page, int size) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "itemId不能为空");
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<Review> reviews = reviewMapper.selectApprovedPageByItemId(itemId, offset, safeSize);
        long total = reviewMapper.countApprovedByItemId(itemId);
        if (reviews.isEmpty()) {
            return PageResult.of(Collections.emptyList(), total, safePage, safeSize);
        }

        Map<Long, List<String>> imageMap = loadImageMap(reviews);
        List<ReviewVO> list = reviews.stream()
                .map(review -> toReviewVO(review, imageMap.getOrDefault(review.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return PageResult.of(list, total, safePage, safeSize);
    }

    private String requireIdempotentKey(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotent-Key不能为空");
        }
        String key = idempotentKey.trim();
        if (key.length() > 128) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotent-Key过长");
        }
        return key;
    }

    private void createProcessingRecord(String key) {
        IdempotentRecord record = new IdempotentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setIdempotentKey(key);
        record.setBizType(BIZ_TYPE_REVIEW);
        record.setBizId(null);
        record.setStatus(IDEMPOTENT_PROCESSING);
        record.setExpireTime(LocalDateTime.now().plusDays(1));
        record.setCreateTime(LocalDateTime.now());
        try {
            idempotentRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            IdempotentRecord existing = idempotentRecordMapper.selectByKey(key);
            if (existing != null && Integer.valueOf(IDEMPOTENT_COMPLETED).equals(existing.getStatus()) && existing.getBizId() != null) {
                throw new BusinessException(ApiCode.CONFLICT, "重复请求，请稍后查询结果");
            }
            throw new BusinessException(ApiCode.CONFLICT, "请求正在处理中");
        }
    }

    private OrderDTO requireReviewableOrder(Long orderId, Long userId) {
        OrderDTO order = getOrder(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权操作该订单");
        }
        if (!Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(order.getStatus())) {
            throw new BusinessException(ApiCode.REVIEW_ORDER_STATUS_INVALID, "订单状态不可评价");
        }
        return order;
    }

    private OrderDTO getOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "orderId不能为空");
        }
        try {
            Result<OrderDTO> result = orderFeignClient.getOrder(orderId);
            if (result == null || result.getCode() != ApiCode.SUCCESS || result.getData() == null) {
                throw new BusinessException(ApiCode.NOT_FOUND, "订单不存在");
            }
            return result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to query order: orderId={}", orderId, ex);
            throw new BusinessException(ApiCode.BIZ_ERROR, "订单服务暂不可用");
        }
    }

    private List<ReviewImage> buildReviewImages(Long reviewId, List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return Collections.emptyList();
        }
        List<ReviewImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = trimToNull(imageUrls.get(i));
            if (imageUrl == null) {
                continue;
            }
            ReviewImage image = new ReviewImage();
            image.setId(snowflakeIdWorker.nextId());
            image.setReviewId(reviewId);
            image.setImageUrl(imageUrl);
            image.setSortOrder(i);
            image.setIsDeleted(NOT_DELETED);
            images.add(image);
        }
        return images;
    }

    private Map<Long, List<String>> loadImageMap(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ReviewImage> images = reviewImageMapper.selectByReviewIds(reviewIds);
        Map<Long, List<String>> imageMap = new LinkedHashMap<>();
        for (ReviewImage image : images) {
            imageMap.computeIfAbsent(image.getReviewId(), ignored -> new ArrayList<>()).add(image.getImageUrl());
        }
        return imageMap;
    }

    private ReviewVO toReviewVO(Review review, List<String> images) {
        ReviewVO vo = new ReviewVO();
        vo.setReviewId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setServiceItemId(review.getServiceItemId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(images);
        vo.setUserNickname(maskUser(review.getUserId()));
        vo.setCreateTime(review.getCreateTime());
        enrichOrderInfo(vo, review.getOrderId());
        return vo;
    }

    private void enrichOrderInfo(ReviewVO vo, Long orderId) {
        try {
            Result<OrderDTO> result = orderFeignClient.getOrder(orderId);
            if (result != null && result.getCode() == ApiCode.SUCCESS && result.getData() != null) {
                vo.setServiceItemName(result.getData().getServiceItemName());
                vo.setSpecName(result.getData().getSpecName());
            }
        } catch (Exception ex) {
            log.warn("Failed to enrich review order info: orderId={}", orderId, ex);
        }
    }

    private String maskUser(Long userId) {
        if (userId == null) {
            return "用户";
        }
        String value = String.valueOf(userId);
        return "用户" + value.substring(Math.max(0, value.length() - 4));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
