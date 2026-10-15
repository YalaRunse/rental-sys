package com.rentalSys.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 缺少所需角色时拒绝访问，不能作为服务器故障返回 500。
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NoRoleInfoException extends RuntimeException {
    public NoRoleInfoException(String message) {
        super(message);
    }
}
