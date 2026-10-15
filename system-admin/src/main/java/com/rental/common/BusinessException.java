package com.rental.common;

import lombok.Getter;

/** 业务拒绝与服务器故障分开，HTTP 状态供前端和测试明确判断。 */
@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    public BusinessException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public static BusinessException bad(String message) { return new BusinessException(400, "INVALID_ARGUMENT", message); }
    public static BusinessException conflict(String message) { return new BusinessException(409, "STATE_CONFLICT", message); }
    public static BusinessException missing(String message) { return new BusinessException(404, "NOT_FOUND", message); }
}
