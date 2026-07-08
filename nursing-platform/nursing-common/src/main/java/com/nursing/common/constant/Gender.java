package com.nursing.common.constant;

public enum Gender {
    SECRET(0, "保密"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int value;
    private final String display;

    Gender(int value, String display) {
        this.value = value;
        this.display = display;
    }

    public int getValue() { return value; }
    public String getDisplay() { return display; }
}
