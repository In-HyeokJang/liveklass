package com.samintech.liveklass.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    List<Enrollment> findByCourseId(Long courseId);

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
