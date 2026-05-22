package com.samintech.liveklass.course;

import com.samintech.liveklass.common.ApiResponse;
import com.samintech.liveklass.enrollment.EnrollmentResponse;
import com.samintech.liveklass.enrollment.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;

/**
 * 강의(Course) 관련 REST API 엔드포인트.
 *
 *Base URL: {@code /api/courses}
 *
 *
 * 메서드 설명 권한
 * POST /api/courses 강의 생성 CREATOR
 * GET /api/courses 강의 목록 조회 누구나
 * GET /api/courses/{id} 강의 상세 조회 누구나
 * PATCH /api/courses/{id}/status 강의 상태 변경 CREATOR(개설자)
 * GET /api/courses/{id}/enrollments 수강생 목록 CREATOR(개설자)
 *
 *
 * 인증은 {@code X-User-Id} 헤더로 처리 헤더가 없으면 400을 반환
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Validated CourseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(courseService.createCourse(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCourses(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean hasVacancies) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.getCourses(status, title, minPrice, maxPrice, startDate, endDate, hasVacancies)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourse(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CourseResponse>> updateStatus(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Validated CourseStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                courseService.updateStatus(id, userId, request.status())));
    }

    // 선택 구현: 강의별 수강생 목록 (크리에이터 전용)
    @GetMapping("/{id}/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getCourseEnrollments(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                enrollmentService.getCourseEnrollments(id, userId)));
    }
}
