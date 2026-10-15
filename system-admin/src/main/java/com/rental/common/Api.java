package com.rental.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.MDC;

/** 统一响应只暴露业务数据与追踪编号，不向客户端输出异常堆栈。 */
@Data
@AllArgsConstructor
public class Api<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private String requestId;

    public static <T> Api<T> ok(T data) {
        return new Api<>(true, "OK", "操作成功", data, MDC.get("requestId"));
    }
    public static Api<Void> error(String code, String message) {
        return new Api<>(false, code, message, null, MDC.get("requestId"));
    }
}
