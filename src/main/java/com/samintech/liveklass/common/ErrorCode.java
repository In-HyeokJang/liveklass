package com.samintech.liveklass.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다"),

    COURSE_NOT_ENROLLABLE(HttpStatus.BAD_REQUEST, "신청 가능한 강의가 아닙니다 (OPEN 상태 아님)"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 변경입니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 이전이어야 합니다"),
    CANCEL_PERIOD_EXCEEDED(HttpStatus.BAD_REQUEST, "취소 가능 기간(결제 후 7일)이 지났습니다"),
    ENROLLMENT_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "취소할 수 없는 수강 신청 상태입니다"),
    ENROLLMENT_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "결제 확정할 수 없는 수강 신청 상태입니다"),

    COURSE_FULL(HttpStatus.CONFLICT, "강의 정원이 초과되었습니다"),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 신청한 강의입니다"),

    NOT_COURSE_CREATOR(HttpStatus.FORBIDDEN, "강의 개설자만 접근할 수 있습니다"),
    NOT_ENROLLMENT_OWNER(HttpStatus.FORBIDDEN, "본인의 수강 신청만 처리할 수 있습니다"),
    UNAUTHORIZED_ROLE(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}