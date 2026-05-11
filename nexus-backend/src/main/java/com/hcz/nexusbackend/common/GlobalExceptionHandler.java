package com.hcz.nexusbackend.common;

import com.hcz.nexusbackend.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusinessException(BusinessException e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", e.getCode());
        result.put("message", e.getMessage());
        return result;
    }
}
