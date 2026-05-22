package com.samintech.liveklass.common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 애플리케이션 전역 예외 처리기.
 *
 * {@code @RestControllerAdvice}는 모든 Controller에서 발생한 예외를 한 곳에서 처리합
 * 각 Controller가 try-catch를 직접 작성하지 않아도 되므로 코드가 깔끔해진다.
 *
 * 처리 우선순위 (위에서 아래로):
 *
 * {@link BusinessException} — 도메인 규칙 위반 (4xx)
 * {@link MethodArgumentNotValidException} — 요청 값 검증 실패 (400)
 * {@link MissingRequestHeaderException} — 필수 헤더 누락 (400)
 * {@link Exception} — 그 외 모든 예외 (500)
 *
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 예외 발생: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("유효성 검증 예외 발생: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("필수 헤더 누락 예외 발생: {}", e.getHeaderName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("요청 본문 누락 또는 파싱 예외 발생: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("요청 본문(Request Body)이 누락되었거나 JSON 형식이 잘못되었습니다"));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("타입 미스매치 예외 발생: {} -> {}", e.getName(), e.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(String.format("파라미터 '%s'의 타입이 잘못되었습니다 (요청값: '%s')", e.getName(), e.getValue())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("서버 내부 오류 발생: ", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
}