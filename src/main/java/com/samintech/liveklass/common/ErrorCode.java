package com.samintech.liveklass.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전체에서 사용하는 에러 코드 열거형.
 *
 * <p>각 상수는 HTTP 상태 코드({@link HttpStatus})와 사람이 읽을 수 있는 메시지를 함께 보유합니다.
 * {@link BusinessException}에 실어 던지면 {@link GlobalExceptionHandler}가
 * 자동으로 올바른 HTTP 응답으로 변환합니다.
 *
 * <p>에러 분류:
 * <ul>
 *   <li>404 NOT_FOUND — 리소스를 찾을 수 없음</li>
 *   <li>400 BAD_REQUEST — 요청 값 또는 상태 전이가 유효하지 않음</li>
 *   <li>409 CONFLICT — 이미 존재하거나 중복된 요청</li>
 *   <li>403 FORBIDDEN — 권한 없음</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다"),

    COURSE_NOT_ENROLLABLE(HttpStatus.BAD_REQUEST, "신청 가능한 강의가 아닙니다."),
    COURSE_EXPIRED(HttpStatus.BAD_REQUEST, "수강 기간이 종료된 강의입니다"),
    COURSE_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "DRAFT 상태의 강의만 수정할 수 있습니다"),
    CAPACITY_BELOW_ENROLLED(HttpStatus.BAD_REQUEST, "현재 수강 인원보다 정원을 줄일 수 없습니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "강의 상태는 DRAFT→OPEN→CLOSED 순서로만 변경 가능합니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다"),
    CANCEL_PERIOD_EXCEEDED(HttpStatus.BAD_REQUEST, "취소 가능 기간(결제 후 7일)이 지났습니다"),
    ENROLLMENT_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "취소할 수 없는 수강 신청 상태입니다"),
    ENROLLMENT_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "결제할 수 없는 수강 신청 상태입니다"),
    ENROLLMENT_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 결제 완료된 강의입니다"),
    ENROLLMENT_WAITLISTED_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "대기 중인 상태에서는 결제를 진행할 수 없습니다"),
    ENROLLMENT_CANCELLED_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "취소된 수강 신청은 결제할 수 없습니다"),

    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 신청한 강의입니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자 이름입니다"),

    NOT_COURSE_CREATOR(HttpStatus.FORBIDDEN, "강의 개설자만 접근할 수 있습니다"),
    NOT_ENROLLMENT_OWNER(HttpStatus.FORBIDDEN, "본인의 수강 신청만 처리할 수 있습니다"),
    UNAUTHORIZED_ROLE(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}