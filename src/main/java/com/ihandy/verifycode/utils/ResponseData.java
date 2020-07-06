package com.ihandy.verifycode.utils;

import java.io.Serializable;

/**
 * 响应接口
 * @param <T>
 */
public class ResponseData<T> implements Serializable{

    private static final long serialVersionUID = 4052817530552594981L;

    /**
     * 请求是否成功 10000 成功 10001 失败
     */
    private int code;

    /**
     * 成功/失败具体消息
     */
    private String msg;

    /**
     * 响应体
     */
    private T responseBody;

    public ResponseData() {
    }

    public ResponseData(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseData(int code, String msg, T responseBody) {
        this.code = code;
        this.msg = msg;
        this.responseBody = responseBody;
    }

    public static ResponseData init(int code, String msg) {
        return new ResponseData(code, msg);
    }

    public static ResponseData init(int code, String msg, Object responseBody) {
        return new ResponseData(code, msg, responseBody);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(T responseBody) {
        this.responseBody = responseBody;
    }
}
