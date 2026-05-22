package com.samintech.liveklass.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // N+1 방지 — user, course를 한 번에 JOIN FETCH
    @EntityGraph(attributePaths = {"user", "course"})
    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "course"})
    List<Enrollment> findByCourseId(Long courseId);

    // 취소 후 재신청 시 기존 CANCELLED row 조회용
    Optional<Enrollment> findByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.status IN :statuses")
    int countByCourseIdAndStatusIn(
            @Param("courseId") Long courseId,
            @Param("statuses") List<EnrollmentStatus> statuses);

    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e WHERE e.course.id IN :courseIds AND e.status IN :statuses GROUP BY e.course.id")
    List<Object[]> countByCourseIdsAndStatusIn(
            @Param("courseIds") List<Long> courseIds,
            @Param("statuses") List<EnrollmentStatus> statuses);

    boolean existsByCourseIdAndUserIdAndStatusIn(
            Long courseId, Long userId, List<EnrollmentStatus> statuses);

    Optional<Enrollment> findFirstByCourseIdAndStatusOrderByEnrolledAtAsc(Long courseId, EnrollmentStatus status);
}
