package com.rental.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;

/** 所有控制器统一处理参数、状态冲突和未预期故障。 */
@Slf4j
@RestControllerAdvice
public class Errors {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Api<Void>> business(BusinessException error) {
        return ResponseEntity.status(error.getStatus()).body(Api.error(error.getCode(), error.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Api<Void>> validation(Exception error) {
        org.springframework.validation.BindingResult result = error instanceof MethodArgumentNotValidException
                ? ((MethodArgumentNotValidException) error).getBindingResult() : ((BindException) error).getBindingResult();
        String message = result.getFieldErrors().stream().findFirst()
                .map(field -> field.getField() + ": " + field.getDefaultMessage()).orElse("参数不合法");
        return ResponseEntity.badRequest().body(Api.error("INVALID_ARGUMENT", message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Api<Void>> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Api.error("INVALID_ARGUMENT", "请求格式或参数类型不正确"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Api<Void>> duplicate(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(Api.error("DATA_CONFLICT", "编号已存在或数据仍被业务引用"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Api<Void>> unexpected(Exception error) {
        log.error("请求处理失败", error);
        return ResponseEntity.status(500).body(Api.error("INTERNAL_ERROR", "操作失败，请凭请求编号联系维护者"));
    }
}
