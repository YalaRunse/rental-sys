package com.rentalSys.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 权限不足属于 403，保留原因供日志和调用者定位。
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NoPermissionInfoException extends RuntimeException {
    public NoPermissionInfoException(String message) {
        super(message);
    }
}
