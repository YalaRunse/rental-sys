package com.rental.config;

import org.slf4j.MDC;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

/** 为每次请求分配追踪号并清理线程上下文，防止线程复用串号。 */
public class TraceFilter implements Filter {
    @Override public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException,ServletException {
        String id = UUID.randomUUID().toString().replace("-","");
        MDC.put("requestId",id); ((HttpServletResponse)response).setHeader("X-Request-Id",id);
        response.setCharacterEncoding("UTF-8");
        try { chain.doFilter(request,response); } finally { MDC.clear(); }
    }
}
