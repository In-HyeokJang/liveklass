package com.samintech.liveklass.enrollment;

import com.samintech.liveklass.common.BusinessException;
import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.course.CourseRepository;
import com.samintech.liveklass.course.CourseStatus;
import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 수강 신청 통합 테스트.
 * 비관적 락(SELECT FOR UPDATE)으로 정원 초과를 방지하는지 검증.
 * PostgreSQL 환경에서의 동작이 권장되며, H2 기본 검증도 포함.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyTest {

    @Autowired EnrollmentService enrollmentService;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    private Course testCourse;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        User creator = userRepository.save(
                User.builder().username("test-creator").role(UserRole.CREATOR).build());

        testCourse = courseRepository.save(Course.builder()
                .creator(creator)
                .title("동시성 테스트 강의")
                .description("정원 1명")
                .price(1000)
                .capacity(1)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .status(CourseStatus.OPEN)
                .build());

        testUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            testUsers.add(userRepository.save(
                    User.builder().username("concurrent-student-" + i).role(UserRole.CLASSMATE).build()));
        }
    }

    @AfterEach
    void tearDown() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 1명인 강의에 10명이 동시 신청 시 1명 PENDING, 9명 WAITLISTED")
    void shouldEnrollOneAndWaitlistRest_whenConcurrentRequests() throws InterruptedException {
        int threadCount = testUsers.size();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final Long userId = testUsers.get(i).getId();
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    enrollmentService.enroll(testCourse.getId(), userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // 대기열 도입 후: 10명 모두 성공 (1명 PENDING, 9명 WAITLISTED)
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        int pendingCount = enrollmentRepository.countByCourseIdAndStatusIn(
                testCourse.getId(), List.of(EnrollmentStatus.PENDING));
        int waitlistedCount = enrollmentRepository.countByCourseIdAndStatusIn(
                testCourse.getId(), List.of(EnrollmentStatus.WAITLISTED));

        assertThat(pendingCount).isEqualTo(1);
        assertThat(waitlistedCount).isEqualTo(threadCount - 1);
    }
}
