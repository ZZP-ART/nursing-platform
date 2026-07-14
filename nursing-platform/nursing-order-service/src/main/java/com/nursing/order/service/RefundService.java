package com.nursing.order.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.PaymentRecord;
import com.nursing.order.entity.RefundRecord;
import com.nursing.order.repository.PaymentRecordMapper;
import com.nursing.order.repository.RefundRecordMapper;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service public class RefundService {
    private final RefundRecordMapper refundMapper; private final PaymentRecordMapper paymentMapper; private final SnowflakeIdWorker ids;
    public RefundService(RefundRecordMapper refundMapper, PaymentRecordMapper paymentMapper, SnowflakeIdWorker ids) { this.refundMapper=refundMapper; this.paymentMapper=paymentMapper; this.ids=ids; }
    public void requestRefund(OrderHeader order) {
        if (refundMapper.selectByOrderId(order.getId()) != null) return;
        PaymentRecord payment = paymentMapper.selectByOrderId(order.getId());
        if (payment == null || !Integer.valueOf(1).equals(payment.getPayStatus())) throw new IllegalStateException("Paid order has no refundable payment record");
        RefundRecord record = new RefundRecord(); record.setId(ids.nextId()); record.setOrderId(order.getId()); record.setPaymentRecordId(payment.getId());
        record.setRefundNo("rf_" + order.getOrderNo()); record.setRefundAmount(payment.getPayAmount()); record.setStatus(0); record.setNextExecuteTime(LocalDateTime.now());
        try { refundMapper.insert(record); } catch (DuplicateKeyException ignored) { }
    }
}
