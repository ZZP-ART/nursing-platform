package com.nursing.common.constant;

public enum TimeSlot {
    MORNING("上午 08:00-12:00"),
    AFTERNOON("下午 13:00-17:00"),
    EVENING("晚上 18:00-21:00");

    private final String display;

    TimeSlot(String display) { this.display = display; }
    public String getDisplay() { return display; }
}
