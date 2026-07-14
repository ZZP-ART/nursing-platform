package com.nursing.feedback.job;
import com.nursing.common.dto.OrderDTO;
import com.nursing.feedback.entity.Review;
import com.nursing.feedback.integration.OrderQueryService;
import com.nursing.feedback.repository.ReviewMapper;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class ReviewSnapshotBackfillJob {
    private static final Logger log = LoggerFactory.getLogger(ReviewSnapshotBackfillJob.class);
    private final ReviewMapper mapper; private final OrderQueryService orders;
    public ReviewSnapshotBackfillJob(ReviewMapper mapper, OrderQueryService orders) { this.mapper=mapper; this.orders=orders; }
    @Scheduled(fixedDelayString="${nursing.review-snapshot.backfill-delay-ms:60000}") public void backfill() {
        try {
            var reviews=mapper.selectMissingSnapshots(100); if(reviews.isEmpty()) return;
            Map<Long,OrderDTO> byId=orders.getOrders(reviews.stream().map(Review::getOrderId).toList()).stream().collect(Collectors.toMap(OrderDTO::getOrderId, Function.identity()));
            for(Review review:reviews) { OrderDTO order=byId.get(review.getOrderId()); if(order!=null) mapper.updateSnapshots(review.getId(),order.getServiceItemName(),order.getSpecName()); }
        } catch (Exception ex) {
            // Leave rows untouched so the next scheduled execution can retry the batch.
            log.warn("Review snapshot backfill will retry after order service failure", ex);
        }
    }
}
