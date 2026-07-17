package com.nursing.operations.entity;

public class OperationIdempotency {
    private Long id;
    private String operation;
    private Long actorUserId;
    private String idempotentKey;
    private String requestHash;
    private Long assignmentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
}
