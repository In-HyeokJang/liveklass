package com.samintech.liveklass.enrollment;

import com.samintech.liveklass.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 수강 신청(Enrollment) 관련 REST API 엔드포인트.
 *
 * Base URL: {@code /api/enrollments}
 * 인증은 {@code X-User-Id} 헤더로 처리합니다. 헤더가 없으면 400을 반환
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Validated EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrollmentService.enroll(request.courseId(), userId)));
    }

    // 선택 구현: 신청 내역 페이지네이션
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 10, sort = "enrolledAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                enrollmentService.getMyEnrollments(userId, pageable)));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> confirm(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.confirm(id, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.cancel(id, userId)));
    }
}
