package com.nursing.common.constant;

public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PENDING_DISPATCH(1, "待派单"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款"),
    ASSIGNED(6, "已派单"),
    ACCEPTED(7, "已接单"),
    IN_SERVICE(8, "服务中"),
    PENDING_CUSTOMER_CONFIRMATION(9, "待用户确认");

    private final int value;
    private final String display;

    OrderStatus(int value, String display) {
        this.value = value;
        this.display = display;
    }

    public int getValue() { return value; }
    public String getDisplay() { return display; }

    public static OrderStatus fromValue(int value) {
        for (OrderStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Unknown OrderStatus: " + value);
    }
}
