package com.nursing.operations.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.constant.OrderStatus;
import com.nursing.common.dto.OrderDTO;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.result.Result;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.operations.dto.request.AssignmentRemarkRequest;
import com.nursing.operations.dto.request.DispatchRequest;
import com.nursing.operations.dto.response.AssignmentResponse;
import com.nursing.operations.dto.response.CaregiverTaskResponse;
import com.nursing.operations.dto.response.MerchantDashboardResponse;
import com.nursing.operations.dto.response.MerchantOrderResponse;
import com.nursing.operations.dto.response.ServiceActionResponse;
import com.nursing.operations.dto.response.CaregiverCandidateResponse;
import com.nursing.operations.entity.CaregiverProfile;
import com.nursing.operations.entity.MerchantMember;
import com.nursing.operations.entity.OperationIdempotency;
import com.nursing.operations.entity.ServiceAssignment;
import com.nursing.operations.entity.ServiceAction;
import com.nursing.operations.feign.OrderServiceFeignClient;
import com.nursing.operations.repository.MerchantMemberMapper;
import com.nursing.operations.repository.OperationIdempotencyMapper;
import com.nursing.operations.repository.ServiceAssignmentMapper;
import com.nursing.operations.repository.CaregiverProfileMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceAssignmentService {
    private static final int WAITING_ACCEPT = 0;
    private static final int ACCEPTED = 1;
    private static final int REJECTED = 2;

    private final MerchantMemberMapper members;
    private final ServiceAssignmentMapper assignments;
    private final OperationIdempotencyMapper idempotencies;
    private final OrderServiceFeignClient orders;
    private final SnowflakeIdWorker ids;
    private final String internalToken;
    private final CaregiverProfileMapper caregiverProfiles;

    public ServiceAssignmentService(MerchantMemberMapper members, ServiceAssignmentMapper assignments,
                                   OperationIdempotencyMapper idempotencies,
                                   OrderServiceFeignClient orders, SnowflakeIdWorker ids,
                                   @Value("${nursing.internal.token:}") String internalToken) {
        this(members, assignments, idempotencies, orders, ids, internalToken, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ServiceAssignmentService(MerchantMemberMapper members, ServiceAssignmentMapper assignments,
                                   OperationIdempotencyMapper idempotencies,
                                   OrderServiceFeignClient orders, SnowflakeIdWorker ids,
                                   @Value("${nursing.internal.token:}") String internalToken,
                                   CaregiverProfileMapper caregiverProfiles) {
        this.members = members;
        this.assignments = assignments;
        this.idempotencies = idempotencies;
        this.orders = orders;
        this.ids = ids;
        this.internalToken = internalToken;
        this.caregiverProfiles = caregiverProfiles;
    }

    @Transactional
    public AssignmentResponse dispatch(Long merchantUserId, Long orderId, DispatchRequest request, String idempotentKey) {
        return executeIdempotent("dispatch", merchantUserId, idempotentKey,
                fingerprint(orderId, request.getCaregiverUserId(), request.getRemark()),
                () -> dispatchInternal(merchantUserId, orderId, request));
    }

    private AssignmentResponse dispatchInternal(Long merchantUserId, Long orderId, DispatchRequest request) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        if (!members.existsActiveCaregiver(member.getMerchantId(), request.getCaregiverUserId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Caregiver is not available for this merchant");
        }
        requireOrderForMerchant(orderId, member.getMerchantId(), OrderStatus.PENDING_DISPATCH.getValue());
        ServiceAssignment active = assignments.selectActiveByOrderId(orderId);
        if (active != null) {
            if (Objects.equals(active.getMerchantId(), member.getMerchantId())
                    && Objects.equals(active.getCaregiverUserId(), request.getCaregiverUserId())) return response(active);
            throw new BusinessException(ApiCode.CONFLICT, "Order has already been dispatched");
        }
        ServiceAssignment assignment = new ServiceAssignment();
        assignment.setId(ids.nextId());
        assignment.setOrderId(orderId);
        assignment.setActiveOrderId(orderId);
        assignment.setMerchantId(member.getMerchantId());
        assignment.setCaregiverUserId(request.getCaregiverUserId());
        assignment.setStatus(WAITING_ACCEPT);
        assignment.setRemark(request.getRemark());
        try {
            assignments.insert(assignment);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ApiCode.CONFLICT, "Order has already been dispatched");
        }
        transition(orderId, OrderStatus.PENDING_DISPATCH.getValue(), OrderStatus.ASSIGNED.getValue(), "Merchant dispatched caregiver");
        return response(assignments.selectById(assignment.getId()));
    }

    public java.util.List<AssignmentResponse> merchantAssignments(Long merchantUserId) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        return assignments.selectByMerchantId(member.getMerchantId()).stream().map(this::response).toList();
    }

    public java.util.List<AssignmentResponse> caregiverAssignments(Long caregiverUserId) {
        return assignments.selectByCaregiverUserId(caregiverUserId).stream().map(this::response).toList();
    }

    public List<CaregiverTaskResponse> caregiverTasks(Long caregiverUserId) {
        return assignments.selectByCaregiverUserId(caregiverUserId).stream()
                .map(assignment -> task(assignment, caregiverUserId, false)).toList();
    }

    public CaregiverTaskResponse caregiverTask(Long caregiverUserId, Long orderId) {
        ServiceAssignment assignment = assignments.selectActiveByOrderId(orderId);
        if (assignment == null || !Objects.equals(assignment.getCaregiverUserId(), caregiverUserId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Assignment does not belong to caregiver");
        }
        return task(assignment, caregiverUserId, true);
    }

    public MerchantDashboardResponse merchantDashboard(Long merchantUserId) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        List<OrderDTO> merchantOrders = merchantOrderDtos(member.getMerchantId(), null);
        long waitingDispatch = merchantOrders.stream().filter(order -> Integer.valueOf(OrderStatus.PENDING_DISPATCH.getValue()).equals(order.getStatus())).count();
        long waitingAccept = assignments.selectByMerchantId(member.getMerchantId()).stream().filter(item -> item.getStatus() == WAITING_ACCEPT).count();
        long todayServices = merchantOrders.stream().filter(order -> LocalDate.now().equals(order.getServiceDate())).count();
        long inService = merchantOrders.stream().filter(order -> Integer.valueOf(OrderStatus.IN_SERVICE.getValue()).equals(order.getStatus())).count();
        long completed = merchantOrders.stream().filter(order -> Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(order.getStatus())).count();
        BigDecimal monthRevenue = merchantOrders.stream()
                .filter(order -> Integer.valueOf(OrderStatus.COMPLETED.getValue()).equals(order.getStatus()))
                .filter(order -> order.getCreateTime() != null && order.getCreateTime().getMonthValue() == LocalDate.now().getMonthValue()
                        && order.getCreateTime().getYear() == LocalDate.now().getYear())
                .map(OrderDTO::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        String merchantName = member.getMerchantId().equals(20001L) ? "康宁居家护理中心" : "Merchant " + member.getMerchantId();
        return new MerchantDashboardResponse(member.getMerchantId(), merchantName, waitingDispatch,
                waitingAccept, todayServices, inService, merchantOrders.size(), completed, monthRevenue, 0);
    }

    public List<MerchantOrderResponse> merchantOrders(Long merchantUserId, Integer status) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        return merchantOrderDtos(member.getMerchantId(), status).stream().map(this::merchantOrder).toList();
    }

    public MerchantOrderResponse merchantOrder(Long merchantUserId, Long orderId) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        OrderDTO order = requireOrderForMerchant(orderId, member.getMerchantId(), null);
        return merchantOrder(order);
    }

    public List<AssignmentResponse> merchantOrderAssignments(Long merchantUserId, Long orderId) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        requireOrderForMerchant(orderId, member.getMerchantId(), null);
        return assignments.selectByMerchantId(member.getMerchantId()).stream()
                .filter(assignment -> Objects.equals(assignment.getOrderId(), orderId)).map(this::response).toList();
    }

    public List<CaregiverCandidateResponse> merchantCandidates(Long merchantUserId, Long orderId) {
        MerchantMember member = requireMerchantMember(merchantUserId);
        requireOrderForMerchant(orderId, member.getMerchantId(), OrderStatus.PENDING_DISPATCH.getValue());
        if (caregiverProfiles == null) throw conflict("Caregiver profiles are unavailable");
        return members.selectActiveCaregiverUserIds(member.getMerchantId()).stream().map(userId -> candidate(member.getMerchantId(), userId)).toList();
    }

    private CaregiverCandidateResponse candidate(Long merchantId, Long caregiverUserId) {
        CaregiverProfile profile = caregiverProfiles.selectByUserId(caregiverUserId);
        int dailyTaskCount = (int) assignments.selectByCaregiverUserId(caregiverUserId).stream()
                .filter(assignment -> assignment.getStatus() != REJECTED)
                .filter(this::isScheduledToday).count();
        List<String> reasons = new java.util.ArrayList<>();
        if (profile == null) reasons.add("缺少护理人员档案");
        else {
            if (!Integer.valueOf(1).equals(profile.getAuditStatus())) reasons.add("护理人员未审核通过");
            if (!"AVAILABLE".equals(profile.getStatus())) reasons.add("护理人员当前不可接单");
            if (profile.getMaxDailyOrders() != null && dailyTaskCount >= profile.getMaxDailyOrders()) reasons.add("已达到每日接单上限");
        }
        return new CaregiverCandidateResponse(caregiverUserId, profile == null ? null : profile.getCaregiverId(),
                profile == null ? null : profile.getRealName(), profile == null ? null : profile.getRating(),
                profile == null ? null : profile.getCompletedOrders(), null, dailyTaskCount,
                profile == null ? null : profile.getMaxDailyOrders(), split(profile == null ? null : profile.getServiceAreas()),
                split(profile == null ? null : profile.getSkills()), reasons.isEmpty(), reasons);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("[、,，]")).map(String::trim).filter(text -> !text.isEmpty()).toList();
    }

    private boolean isScheduledToday(ServiceAssignment assignment) {
        Result<OrderDTO> result = orders.getOrder(assignment.getOrderId(), internalToken);
        return result != null && result.getCode() == 0 && result.getData() != null
                && LocalDate.now().equals(result.getData().getServiceDate());
    }

    private List<OrderDTO> merchantOrderDtos(Long merchantId, Integer status) {
        Result<List<OrderDTO>> result = orders.getMerchantOrders(merchantId, status, internalToken);
        if (result == null || result.getCode() != 0 || result.getData() == null) {
            throw conflict("Merchant orders are unavailable");
        }
        return result.getData();
    }

    private MerchantOrderResponse merchantOrder(OrderDTO order) {
        ServiceAssignment assignment = assignments.selectActiveByOrderIdAndMerchantId(order.getOrderId(), order.getMerchantId());
        AssignmentResponse current = assignment == null ? null : response(assignment);
        List<AssignmentResponse> orderAssignments = assignments.selectByMerchantId(order.getMerchantId()).stream()
                .filter(item -> Objects.equals(item.getOrderId(), order.getOrderId())).map(this::response).toList();
        List<ServiceActionResponse> records = assignment == null ? List.of()
                : assignments.selectActionsByAssignmentId(assignment.getId()).stream()
                .map(item -> new ServiceActionResponse(item.getAction(), item.getRemark(), item.getCreateTime())).toList();
        return new MerchantOrderResponse(order.getOrderId(), order.getOrderNo(), order.getStatus(),
                assignment == null ? null : assignment.getStatus(), order.getServiceItemName(), order.getSpecName(),
                order.getTotalAmount(), order.getServiceDate(), order.getServiceTimeSlot(), order.getReceiverName(),
                order.getReceiverPhone(), order.getAddressDetail(), current, orderAssignments, records, order.getCreateTime());
    }

    private CaregiverTaskResponse task(ServiceAssignment assignment, Long caregiverUserId, boolean includeActions) {
        OrderDTO order = requireOrderForMerchant(assignment.getOrderId(), assignment.getMerchantId(), null);
        if (!Objects.equals(assignment.getCaregiverUserId(), caregiverUserId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Assignment does not belong to caregiver");
        }
        List<ServiceActionResponse> records = assignments.selectActionsByAssignmentId(assignment.getId()).stream()
                .map(action -> new ServiceActionResponse(action.getAction(), action.getRemark(), action.getCreateTime())).toList();
        List<String> actions = includeActions ? availableActions(assignment, order, records) : List.of();
        return new CaregiverTaskResponse(assignment.getId(), order.getOrderId(), order.getOrderNo(), assignment.getMerchantId(),
                caregiverUserId, assignment.getStatus(), order.getStatus(), order.getServiceItemId(), order.getServiceItemName(),
                order.getSpecName(), order.getServiceDate(), order.getServiceTimeSlot(), order.getReceiverName(),
                order.getReceiverPhone(), order.getAddressDetail(), order.getRemark(), order.getTotalAmount(), order.getCreateTime(), records, actions);
    }

    private List<String> availableActions(ServiceAssignment assignment, OrderDTO order, List<ServiceActionResponse> records) {
        if (assignment.getStatus() == WAITING_ACCEPT && order.getStatus().equals(OrderStatus.ASSIGNED.getValue())) return List.of("accept", "reject");
        if (assignment.getStatus() != ACCEPTED) return List.of();
        List<String> completed = records.stream().map(ServiceActionResponse::action).toList();
        if (!completed.contains("depart")) return List.of("depart");
        if (!completed.contains("check-in")) return List.of("check-in");
        if (!completed.contains("start") && order.getStatus().equals(OrderStatus.ACCEPTED.getValue())) return List.of("start");
        if (!completed.contains("finish") && order.getStatus().equals(OrderStatus.IN_SERVICE.getValue())) return List.of("finish");
        return List.of();
    }

    @Transactional
    public AssignmentResponse accept(Long caregiverUserId, Long assignmentId, String idempotentKey) {
        return executeIdempotent("accept", caregiverUserId, idempotentKey, fingerprint(assignmentId),
                () -> acceptInternal(caregiverUserId, assignmentId));
    }

    private AssignmentResponse acceptInternal(Long caregiverUserId, Long assignmentId) {
        ServiceAssignment assignment = requireAssignment(assignmentId, caregiverUserId);
        if (assignment.getStatus() == ACCEPTED) return response(assignment);
        if (assignment.getStatus() != WAITING_ACCEPT) throw conflict("Assignment cannot be accepted");
        if (assignments.accept(assignmentId, caregiverUserId) != 1) throw conflict("Assignment was already handled");
        transition(assignment.getOrderId(), OrderStatus.ASSIGNED.getValue(), OrderStatus.ACCEPTED.getValue(), "Caregiver accepted assignment");
        return response(assignments.selectById(assignmentId));
    }

    @Transactional
    public AssignmentResponse reject(Long caregiverUserId, Long assignmentId, AssignmentRemarkRequest request, String idempotentKey) {
        return executeIdempotent("reject", caregiverUserId, idempotentKey,
                fingerprint(assignmentId, request == null ? null : request.getRemark()),
                () -> rejectInternal(caregiverUserId, assignmentId, request));
    }

    private AssignmentResponse rejectInternal(Long caregiverUserId, Long assignmentId, AssignmentRemarkRequest request) {
        ServiceAssignment assignment = requireAssignment(assignmentId, caregiverUserId);
        if (assignment.getStatus() == REJECTED) return response(assignment);
        if (assignment.getStatus() != WAITING_ACCEPT) throw conflict("Assignment cannot be rejected");
        if (assignments.reject(assignmentId, caregiverUserId, request == null ? null : request.getRemark()) != 1) {
            throw conflict("Assignment was already handled");
        }
        transition(assignment.getOrderId(), OrderStatus.ASSIGNED.getValue(), OrderStatus.PENDING_DISPATCH.getValue(), "Caregiver rejected assignment");
        return response(assignments.selectById(assignmentId));
    }

    @Transactional
    public AssignmentResponse recordAction(Long caregiverUserId, Long orderId, String action,
                                           AssignmentRemarkRequest request, String idempotentKey) {
        return executeIdempotent("action", caregiverUserId, idempotentKey,
                fingerprint(orderId, action, request == null ? null : request.getRemark()),
                () -> recordActionInternal(caregiverUserId, orderId, action, request));
    }

    private AssignmentResponse recordActionInternal(Long caregiverUserId, Long orderId, String action,
                                                     AssignmentRemarkRequest request) {
        ServiceAssignment assignment = assignments.selectActiveByOrderId(orderId);
        if (assignment == null || !Objects.equals(assignment.getCaregiverUserId(), caregiverUserId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Assignment does not belong to caregiver");
        }
        if (assignment.getStatus() != ACCEPTED) throw conflict("Assignment has not been accepted");
        Action expected = Action.fromPath(action);
        if (assignments.hasAction(assignment.getId(), expected.value)) return response(assignment);
        if (assignments.countActions(assignment.getId()) != expected.position) throw conflict("Service actions are out of order");
        assignments.insertAction(ids.nextId(), assignment.getId(), expected.value, caregiverUserId,
                request == null ? null : request.getRemark());
        if (expected == Action.START) {
            transition(orderId, OrderStatus.ACCEPTED.getValue(), OrderStatus.IN_SERVICE.getValue(), "Caregiver started service");
        } else if (expected == Action.FINISH) {
            transition(orderId, OrderStatus.IN_SERVICE.getValue(), OrderStatus.PENDING_CUSTOMER_CONFIRMATION.getValue(), "Caregiver finished service");
        }
        return response(assignments.selectById(assignment.getId()));
    }

    private MerchantMember requireMerchantMember(Long userId) {
        MerchantMember member = members.selectEnabledByUserId(userId);
        if (member == null) throw new BusinessException(ApiCode.FORBIDDEN, "Merchant membership is required");
        return member;
    }

    private AssignmentResponse executeIdempotent(String operation, Long actorUserId, String idempotentKey,
                                                   String requestHash, Supplier<AssignmentResponse> command) {
        OperationIdempotency existing = idempotencies.select(operation, actorUserId, idempotentKey);
        if (existing != null) return replay(existing, requestHash);

        OperationIdempotency record = new OperationIdempotency();
        record.setId(ids.nextId());
        record.setOperation(operation);
        record.setActorUserId(actorUserId);
        record.setIdempotentKey(idempotentKey);
        record.setRequestHash(requestHash);
        try {
            idempotencies.insert(record);
        } catch (DuplicateKeyException ex) {
            OperationIdempotency concurrent = idempotencies.select(operation, actorUserId, idempotentKey);
            if (concurrent != null) return replay(concurrent, requestHash);
            throw conflict("Idempotent request is being processed");
        }
        AssignmentResponse result = command.get();
        if (idempotencies.complete(record.getId(), result.assignmentId()) != 1) {
            throw conflict("Idempotent request could not be completed");
        }
        return result;
    }

    private AssignmentResponse replay(OperationIdempotency record, String requestHash) {
        if (!Objects.equals(record.getRequestHash(), requestHash)) {
            throw conflict("Idempotency-Key cannot be reused with a different request");
        }
        if (record.getAssignmentId() == null) throw conflict("Idempotent request is being processed");
        ServiceAssignment assignment = assignments.selectById(record.getAssignmentId());
        if (assignment == null) throw conflict("Idempotent result is unavailable");
        return response(assignment);
    }

    private String fingerprint(Object... parts) {
        StringBuilder value = new StringBuilder();
        for (Object part : parts) value.append(part == null ? "<null>" : part).append('\u001f');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ServiceAssignment requireAssignment(Long assignmentId, Long caregiverUserId) {
        ServiceAssignment assignment = assignments.selectById(assignmentId);
        if (assignment == null || !Objects.equals(assignment.getCaregiverUserId(), caregiverUserId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Assignment does not belong to caregiver");
        }
        return assignment;
    }

    private OrderDTO requireOrderForMerchant(Long orderId, Long merchantId, Integer expected) {
        Result<OrderDTO> result = orders.getOrder(orderId, internalToken);
        if (result == null || result.getCode() != 0 || result.getData() == null) {
            throw conflict("Order is unavailable");
        }
        OrderDTO order = result.getData();
        if (!Objects.equals(order.getMerchantId(), merchantId)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "Order does not belong to merchant");
        }
        if (expected != null && !Objects.equals(order.getStatus(), expected)) {
            throw conflict("Order is not in the required status");
        }
        return order;
    }

    private void transition(Long orderId, int from, int to, String reason) {
        try {
            Result<OrderDTO> result = orders.transition(orderId,
                    Map.of("fromStatus", from, "toStatus", to, "reason", reason), internalToken);
            if (result != null && result.getCode() == 0) return;
        } catch (RuntimeException ignored) {
            // The remote commit can succeed before a response timeout reaches this service.
        }
        Result<OrderDTO> latest = orders.getOrder(orderId, internalToken);
        if (latest != null && latest.getCode() == 0 && latest.getData() != null
                && Objects.equals(latest.getData().getStatus(), to)) return;
        throw conflict("Order status changed concurrently");
    }

    private AssignmentResponse response(ServiceAssignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getOrderId(), assignment.getMerchantId(),
                assignment.getCaregiverUserId(), assignment.getStatus(), assignment.getAcceptedTime(), assignment.getRejectedTime());
    }

    private BusinessException conflict(String message) { return new BusinessException(ApiCode.CONFLICT, message); }

    private enum Action {
        DEPART("depart", 0), CHECK_IN("check-in", 1), START("start", 2), FINISH("finish", 3);
        private final String value;
        private final int position;
        Action(String value, int position) { this.value = value; this.position = position; }
        static Action fromPath(String value) {
            for (Action action : values()) if (action.value.equals(value)) return action;
            throw new BusinessException(ApiCode.PARAM_ERROR, "Unsupported service action");
        }
    }
}
