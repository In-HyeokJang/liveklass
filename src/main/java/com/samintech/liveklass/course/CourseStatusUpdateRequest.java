package com.samintech.liveklass.course;

import jakarta.validation.constraints.NotNull;

/**
 * 강의 상태 변경 요청 DTO.
 *
 * 크리에이터가 강의 상태를 변경할 때 사용
 * 허용되는 전이: {@code DRAFT → OPEN}, {@code OPEN → CLOSED}.
 * 유효하지 않은 전이는 Service 계층에서 거부
 *
 * @param status 변경하고자 하는 목표 상태 (필수)
 */
public record CourseStatusUpdateRequest(
        @NotNull(message = "변경할 상태는 필수입니다") CourseStatus status
) {}
