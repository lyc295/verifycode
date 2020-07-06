package com.ihandy.verifycode.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 响应枚举
 */
public enum ResponseCode {

    SUCCESS(10000, "成功"),
    FAIL(10001, "失败"),
    ABNORMAL(10002,"异常"),
    NO_LOGIN(10003, "未登录"),
    NO_SESSION(10004, "登录已过期");

    private Integer value;

    private String desc;

    ResponseCode(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    private static Map<Integer, ResponseCode> map;

    static {
        map = new HashMap<>(16);
        for (ResponseCode status : ResponseCode.values()) {
            map.put(status.value, status);
        }
    }

    public Integer getValue() {
        return value;
    }

    public Short getShortValue() {
        return value.shortValue();
    }

    public String getDesc() {
        return desc;
    }

    public static ResponseCode getByValue(Integer value) {
        return map.get(value);
    }
}
