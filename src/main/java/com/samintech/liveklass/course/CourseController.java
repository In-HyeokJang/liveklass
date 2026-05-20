package com.samintech.liveklass.course;

import com.samintech.liveklass.common.ApiResponse;
import com.samintech.liveklass.enrollment.EnrollmentResponse;
import com.samintech.liveklass.enrollment.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestParam(required = false) CourseStatus status) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourses(status)));
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
