package com.nursing.common.constant;

public enum ReviewStatus {
    PENDING_REVIEW("待审核"),
    APPROVED("已展示"),
    HIDDEN("隐藏");

    private final String display;

    ReviewStatus(String display) { this.display = display; }
    public String getDisplay() { return display; }
}
