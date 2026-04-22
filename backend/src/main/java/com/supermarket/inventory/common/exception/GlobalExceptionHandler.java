package com.supermarket.inventory.common.exception;

import com.supermarket.inventory.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleValidationException(Exception exception) {
        String message = "请求参数错误";
        if (exception instanceof MethodArgumentNotValidException validException
                && validException.getBindingResult().hasFieldErrors()) {
            message = validException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        } else if (exception instanceof BindException bindException
                && bindException.getBindingResult().hasFieldErrors()) {
            message = bindException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        }
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResponse.error(400, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        return ApiResponse.error(400, "数据约束校验失败，请检查唯一性、外键或数值范围");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.error(500, "系统内部异常");
    }
}
