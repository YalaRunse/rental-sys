package com.rental.auth;

import java.lang.annotation.*;

/** 声明接口需要的权限，所有检查由服务端认证拦截器执行。 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Require { String value(); }
