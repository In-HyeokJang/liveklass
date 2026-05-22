package com.samintech.liveklass.enrollment;

/**
 * 수강 신청(Enrollment)의 처리 상태를 나타내는 열거형.
 *
 * 일반적인 상태 흐름:
 *
 *   PENDING → CONFIRMED  (수강생이 결제 확정)
 *   PENDING → CANCELLED  (수강생이 결제 전 취소)
 *   CONFIRMED → CANCELLED (결제 후 7일 이내 취소)
 *   WAITLISTED → PENDING  (앞 수강생 취소 시 자동 승격)
 *
 *
 * 정원(capacity) 계산에는 {@code PENDING}과 {@code CONFIRMED}만 포함
 * {@code WAITLISTED}는 자리를 차지하지 않으며, 자리가 생기면 자동으로 {@code PENDING}으로 승격
 */
public enum EnrollmentStatus {

    /** 결제 대기 중. 자리는 확보됐지만 아직 결제가 완료되지 않은 상태 */
    PENDING,

    /** 결제 완료. 수강이 확정된 상태. 확정 후 7일 이내에만 취소 가능 */
    CONFIRMED,

    /** 취소됨. 수강생이 직접 취소하거나 기간 초과로 처리된 상태 */
    CANCELLED,

    /** 대기열 등록. 강의 정원이 가득 찼을 때 등록되는 상태. 자리가 생기면 PENDING으로 승격됨 */
    WAITLISTED
}