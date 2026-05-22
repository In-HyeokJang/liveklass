package com.samintech.liveklass.user;

/**
 * 사용자 역할(Role)을 나타내는 열거형.
 *
 * 이 시스템에는 두 종류의 사용자가 존재
 *
 *   {@code CREATOR} — 강의를 만들고 상태를 관리하는 강사
 *   {@code CLASSMATE} — 강의를 검색하고 수강 신청하는 수강생
 *
 *
 * 역할에 따라 접근 가능한 API가 다르다.
 * 예를 들어 강의 생성은 CREATOR만, 수강 신청은 CLASSMATE만 가능
 * 권한 검증은 각 Service 레이어에서 수행
 */
public enum UserRole {

    /** 강의를 개설하고 상태(DRAFT→OPEN→CLOSED)를 관리하는 강사 역할 */
    CREATOR,

    /** 강의를 수강 신청·결제·취소하는 수강생 역할 */
    CLASSMATE
}