package com.example.travelplanner.common.response;

import com.example.travelplanner.common.exception.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 모든 API 응답을 감싸는 공통 응답 포맷.
 * 컨트롤러는 도메인 객체를 직접 반환하지 않고 항상 이 래퍼를 통해 반환한다.
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "OK", "요청이 성공적으로 처리되었습니다.", null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
