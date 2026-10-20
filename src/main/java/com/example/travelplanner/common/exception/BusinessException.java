package com.example.travelplanner.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 공통 예외.
 * 서비스 계층에서는 이 예외(또는 하위 클래스)만 던지고,
 * HTTP 응답 변환은 GlobalExceptionHandler가 전담한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
