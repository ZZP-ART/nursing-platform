package com.nursing.common.constant;

public enum ComplaintStatus {
    PENDING("待处理"),
    PROCESSING("处理中"),
    RESOLVED("已处理"),
    CLOSED("已关闭");

    private final String display;

    ComplaintStatus(String display) { this.display = display; }
    public String getDisplay() { return display; }
}
