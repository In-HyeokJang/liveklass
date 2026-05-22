package com.samintech.liveklass.enrollment;

import jakarta.validation.constraints.NotNull;

/**
 * 수강 신청 요청 DTO.
 *
 * 신청할 강의 ID만 받고, 사용자 ID는 헤더({@code X-User-Id})에서 가져오므로
 * 요청 본문에 포함하지 않는다
 *
 * @param courseId 수강 신청할 강의의 ID (필수)
 */
public record EnrollmentRequest(
        @NotNull(message = "강의 ID는 필수입니다") Long courseId
) {}
