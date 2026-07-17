package com.nursing.feedback.service.impl;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.PageResult;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.feedback.dto.request.SubmitReviewRequest;
import com.nursing.feedback.dto.response.ReviewSubmitResponse;
import com.nursing.feedback.dto.response.ReviewVO;
import com.nursing.feedback.entity.IdempotentRecord;
import com.nursing.feedback.entity.Review;
import com.nursing.feedback.entity.ReviewEligibility;
import com.nursing.feedback.entity.ReviewImage;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.IdempotentRecordMapper;
import com.nursing.feedback.repository.ReviewImageMapper;
import com.nursing.feedback.repository.ReviewEligibilityMapper;
import com.nursing.feedback.repository.ReviewMapper;
import com.nursing.feedback.support.RequestFingerprint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ReviewServiceImpl implements com.nursing.feedback.service.ReviewService {
    private static final String BIZ_TYPE_REVIEW = "REVIEW";
    private static final int REVIEW_STATUS_PENDING = 1;
    private static final int NOT_DELETED = 0;
    private static final int IDEMPOTENT_PROCESSING = 0;
    private static final int IDEMPOTENT_COMPLETED = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewEligibilityMapper reviewEligibilityMapper;
    private final IdempotentRecordMapper idempotentRecordMapper;
    private final OrderQueryService orderQueryService;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             ReviewImageMapper reviewImageMapper,
                             ReviewEligibilityMapper reviewEligibilityMapper,
                             IdempotentRecordMapper idempotentRecordMapper,
                             OrderQueryService orderQueryService,
                             SnowflakeIdWorker snowflakeIdWorker) {
        this.reviewMapper = reviewMapper;
        this.reviewImageMapper = reviewImageMapper;
        this.reviewEligibilityMapper = reviewEligibilityMapper;
        this.idempotentRecordMapper = idempotentRecordMapper;
        this.orderQueryService = orderQueryService;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewSubmitResponse submitReview(SubmitReviewRequest request, Long userId, String idempotentKey) {
        String key = requireIdempotentKey(idempotentKey);
        String content = requireContent(request.getContent());
        String requestHash = RequestFingerprint.review(request);
        LocalDateTime now = LocalDateTime.now();
        idempotentRecordMapper.deleteExpiredByScope(BIZ_TYPE_REVIEW, userId, key, now);
        IdempotentRecord existingRecord = idempotentRecordMapper.selectByScope(BIZ_TYPE_REVIEW, userId, key);
        if (existingRecord != null) {
            return replayExisting(existingRecord, requestHash);
        }

        try {
            createProcessingRecord(key, userId, requestHash, now);
        } catch (DuplicateKeyException ex) {
            IdempotentRecord duplicate = idempotentRecordMapper.selectByScope(BIZ_TYPE_REVIEW, userId, key);
            if (duplicate != null) {
                return replayExisting(duplicate, requestHash);
            }
            throw new BusinessException(ApiCode.CONFLICT, "Duplicate request");
        }
        OrderDTO order = requireReviewableOrder(request.getOrderId(), userId);
        Review duplicate = reviewMapper.selectByOrderId(request.getOrderId());
        if (duplicate != null) {
            throw new BusinessException(ApiCode.REVIEW_DUPLICATE, "Order already reviewed");
        }

        Long reviewId = snowflakeIdWorker.nextId();
        Review review = new Review();
        review.setId(reviewId);
        review.setOrderId(request.getOrderId());
        review.setUserId(userId);
        review.setServiceItemId(order.getServiceItemId());
        review.setServiceItemName(order.getServiceItemName());
        review.setSpecName(order.getSpecName());
        review.setRating(request.getRating());
        review.setContent(content);
        review.setStatus(REVIEW_STATUS_PENDING);
        review.setIsDeleted(NOT_DELETED);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        reviewMapper.insert(review);

        List<ReviewImage> images = buildReviewImages(reviewId, request.getImages());
        if (!images.isEmpty()) {
            reviewImageMapper.insertBatch(images);
        }
        idempotentRecordMapper.updateCompleted(BIZ_TYPE_REVIEW, userId, key, reviewId);
        return new ReviewSubmitResponse(reviewId);
    }

    @Override
    public PageResult<ReviewVO> pageReviews(Long itemId, int page, int size) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "itemId is required");
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
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotency-Key is required");
        }
        String key = idempotentKey.trim();
        if (key.length() > 128) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "Idempotency-Key is too long");
        }
        return key;
    }

    private ReviewSubmitResponse replayExisting(IdempotentRecord record, String requestHash) {
        if (!Objects.equals(record.getRequestHash(), requestHash)) {
            throw new BusinessException(ApiCode.CONFLICT, "Idempotency-Key was reused with a different request");
        }
        if (Integer.valueOf(IDEMPOTENT_COMPLETED).equals(record.getStatus()) && record.getBizId() != null) {
            return new ReviewSubmitResponse(record.getBizId());
        }
        throw new BusinessException(ApiCode.CONFLICT, "Request is processing");
    }

    private void createProcessingRecord(String key, Long userId, String requestHash, LocalDateTime now) {
        IdempotentRecord record = new IdempotentRecord();
        record.setId(snowflakeIdWorker.nextId());
        record.setBizType(BIZ_TYPE_REVIEW);
        record.setSubjectId(userId);
        record.setIdempotentKey(key);
        record.setRequestHash(requestHash);
        record.setBizId(null);
        record.setStatus(IDEMPOTENT_PROCESSING);
        record.setExpireTime(now.plusDays(1));
        record.setCreateTime(now);
        idempotentRecordMapper.insert(record);
    }

    private OrderDTO requireReviewableOrder(Long orderId, Long userId) {
        OrderDTO order = getOrder(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "No permission for this order");
        }
        if (!Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(order.getStatus())) {
            throw new BusinessException(ApiCode.REVIEW_ORDER_STATUS_INVALID, "Order status cannot be reviewed");
        }
        ReviewEligibility eligibility = reviewEligibilityMapper.selectByOrderIdAndUserId(orderId, userId);
        if (eligibility == null || !Objects.equals(eligibility.getServiceItemId(), order.getServiceItemId())) {
            throw new BusinessException(ApiCode.REVIEW_ORDER_STATUS_INVALID, "Order is not eligible for review");
        }
        return order;
    }

    private OrderDTO getOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ApiCode.PARAM_ERROR, "orderId is required");
        }
        OrderDTO order = orderQueryService.getOrder(orderId);
        if (order == null) {
            throw new BusinessException(ApiCode.NOT_FOUND, "Order not found");
        }
        return order;
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
        vo.setServiceItemName(review.getServiceItemName());
        vo.setSpecName(review.getSpecName());
        return vo;
    }

    private String maskUser(Long userId) {
        if (userId == null) {
            return "user";
        }
        String value = String.valueOf(userId);
        return "user" + value.substring(Math.max(0, value.length() - 4));
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
            throw new BusinessException(ApiCode.PARAM_ERROR, "Review content is required");
        }
        return normalized;
    }
}
