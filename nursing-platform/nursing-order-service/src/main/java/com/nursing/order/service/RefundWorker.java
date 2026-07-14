package com.nursing.order.service;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.entity.OrderHeader;
import com.nursing.order.entity.OrderOperationLog;
import com.nursing.order.entity.RefundRecord;
import com.nursing.order.repository.OrderHeaderMapper;
import com.nursing.order.repository.OrderOperationLogMapper;
import com.nursing.order.repository.PaymentRecordMapper;
import com.nursing.order.repository.RefundRecordMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component public class RefundWorker {
    private static final Logger log = LoggerFactory.getLogger(RefundWorker.class);
    private final RefundRecordMapper refunds; private final PaymentRecordMapper payments; private final OrderHeaderMapper orders; private final OrderOperationLogMapper logs; private final PaymentRefundGateway gateway; private final SnowflakeIdWorker ids; private final TransactionTemplate tx; private final int maxRetries; private final long retryBaseSeconds; private final String owner=UUID.randomUUID().toString();
    public RefundWorker(RefundRecordMapper refunds, PaymentRecordMapper payments, OrderHeaderMapper orders, OrderOperationLogMapper logs, PaymentRefundGateway gateway, SnowflakeIdWorker ids, TransactionTemplate tx, @Value("${nursing.refund.max-retries:5}") int maxRetries, @Value("${nursing.refund.retry-base-seconds:30}") long retryBaseSeconds) { this.refunds=refunds;this.payments=payments;this.orders=orders;this.logs=logs;this.gateway=gateway;this.ids=ids;this.tx=tx;this.maxRetries=Math.max(1,maxRetries);this.retryBaseSeconds=Math.max(1,retryBaseSeconds); }
    @Scheduled(fixedDelayString="${nursing.refund.fixed-delay-ms:5000}") public void dispatch() { refunds.selectPending(50, LocalDateTime.now()).forEach(this::process); }
    private void process(RefundRecord record) {
        LocalDateTime now=LocalDateTime.now(); if(refunds.claim(record.getId(),owner,now.plusMinutes(2),now)!=1) return;
        try { String providerNo=gateway.refund(record); tx.executeWithoutResult(s -> complete(record, providerNo)); }
        catch(Exception ex) {
            String error=truncate(ex.getMessage()); int attempt=record.getRetryCount()+1;
            if(attempt >= maxRetries) { refunds.markManual(record.getId(),owner,error); log.error("Refund requires manual handling: refundId={}, orderId={}, attempts={}",record.getId(),record.getOrderId(),attempt,ex); return; }
            long delay=Math.min(retryBaseSeconds * (1L << Math.min(record.getRetryCount(), 6)), 3600L);
            refunds.retry(record.getId(),owner,LocalDateTime.now().plusSeconds(delay),error);
        }
    }
    private void complete(RefundRecord record, String providerNo) {
        if(refunds.markSucceeded(record.getId(),owner,providerNo)!=1) return;
        payments.markRefunded(record.getPaymentRecordId(),LocalDateTime.now()); OrderHeader order=orders.selectById(record.getOrderId());
        if(order != null && Integer.valueOf(4).equals(order.getStatus()) && orders.updateStatusByIdVersion(order.getId(),4,5,order.getVersion(),null)==1) { OrderOperationLog log=new OrderOperationLog();log.setId(ids.nextId());log.setOrderId(order.getId());log.setOrderNo(order.getOrderNo());log.setUserId(order.getUserId());log.setOperator("SYSTEM");log.setAction("refund_complete");log.setFromStatus(4);log.setToStatus(5);log.setRemark(providerNo);logs.insert(log); }
    }
    private String truncate(String value){return value==null?"refund gateway failure":value.substring(0,Math.min(512,value.length()));}
}
