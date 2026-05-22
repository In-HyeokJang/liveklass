package com.samintech.liveklass.common;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 발생하는 예외.
 *
 * NullPointerException, IllegalArgumentException 같은 기술적 예외와 구분하기 위해
 * 별도의 예외 클래스를 사용합니다. 모든 도메인/서비스 계층의 규칙 위반은 이 예외로 표현
 *
 * 어떤 종류의 규칙 위반인지는 {@link ErrorCode}가 담고 있으며,
 * {@link GlobalExceptionHandler}가 이 예외를 잡아 적절한 HTTP 응답(4xx)으로 변환
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}