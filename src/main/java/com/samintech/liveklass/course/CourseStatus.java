package com.samintech.liveklass.course;

/**
 *
 * 강의는 다음 순서로만 상태가 전이
 *   DRAFT → OPEN → CLOSED
 *
 * 역방향 전이(예: OPEN → DRAFT)나 단계 건너뛰기(예: DRAFT → CLOSED)는
 * {@link Course#transitionTo(CourseStatus)}에서 예외를 던져 막음
 * 수강 신청은 {@code OPEN} 상태인 강의에만 가능
 */
public enum CourseStatus {

    /** 초안 상태. 크리에이터가 강의를 만들었지만 아직 공개하지 않은 상태 */
    DRAFT,

    /** 모집 중 상태. 수강생이 신청할 수 있는 공개 상태 */
    OPEN,

    /** 모집 마감 상태. 더 이상 수강 신청을 받지 않음 */
    CLOSED
}