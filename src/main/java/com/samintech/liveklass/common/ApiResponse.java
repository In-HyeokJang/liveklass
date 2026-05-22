package com.samintech.liveklass.common;

import lombok.Getter;

/**
 * 공통 래퍼 클래스.
 *
 * 성공/실패 여부, 실제 데이터, 에러 메시지를 일관된 구조로 반환
 * 성공: { "success": true,  "data": {...}, "message": null  }
 * 실패: { "success": false, "data": null,  "message": "..." }
 *
 * 인스턴스는 정적 팩토리 메서드({@link #success(Object)}, {@link #error(String)})로만 생성
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}