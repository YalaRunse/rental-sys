package com.rentalSys.constant;

public class Constants {
    //统一的token前缀
    public static final String TOKEN_PREFIX = "token:";

    public static final String HEAD_AUTHORIZATION = "Authorization";

    //token的过期时间
    public static final Long TOKEN_TIME = 30*60L;
    public static final String ROLE_PREFIX = "roles:";
    public static final String PERMS_PREFIX = "perm:";
}
