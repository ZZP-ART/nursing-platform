package com.nursing.common.constant;

public enum ComplaintType {
    SERVICE_QUALITY("服务质量"),
    SERVICE_ATTITUDE("服务态度"),
    OVERCHARGING("乱收费"),
    OTHER("其他");

    private final String display;

    ComplaintType(String display) { this.display = display; }
    public String getDisplay() { return display; }
}
