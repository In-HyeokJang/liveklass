package com.samintech.liveklass;

import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.course.CourseRepository;
import com.samintech.liveklass.course.CourseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 서버 기동 시 만료된 강의를 일괄 CLOSED 처리하는 컴포넌트.
 *
 * 스케줄러 단독 방식은 서버 다운 중 누락이 발생할 수 있으므로,
 * 재기동 시점에 한 번 실행하여 서버 중단 기간의 누락분을 보완한다.
 * EnrollmentService.enroll()의 isExpired() 체크와 함께 2-레이어 방어를 구성한다.
 */
@Slf4j
@Component
@Order(2)
@Profile("!test")
@RequiredArgsConstructor
public class CourseExpiryCloser implements CommandLineRunner {

    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Course> expired = courseRepository.findByStatusAndEndDateBefore(
                CourseStatus.OPEN, LocalDate.now());

        if (expired.isEmpty()) return;

        expired.forEach(c -> c.transitionTo(CourseStatus.CLOSED));
        log.info("만료 강의 {}건 CLOSED 처리 완료 (종료일 경과)", expired.size());
    }
}