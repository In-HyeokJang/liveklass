package com.samintech.liveklass;

import com.samintech.liveklass.course.Course;
import com.samintech.liveklass.course.CourseRepository;
import com.samintech.liveklass.course.CourseStatus;
import com.samintech.liveklass.enrollment.Enrollment;
import com.samintech.liveklass.enrollment.EnrollmentRepository;
import com.samintech.liveklass.enrollment.EnrollmentStatus;
import com.samintech.liveklass.user.User;
import com.samintech.liveklass.user.UserRepository;
import com.samintech.liveklass.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(1)
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("시드 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("시드 데이터 초기화 시작...");

        // 1. 강사(CREATOR) 5명 등록 -> ID: 1 ~ 5
        List<User> creators = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            creators.add(userRepository.save(
                    User.builder()
                            .username("creator" + i)
                            .role(UserRole.CREATOR)
                            .build()
            ));
        }

        // 2. 학생(CLASSMATE) 15명 등록 -> ID: 6 ~ 20
        List<User> students = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            students.add(userRepository.save(
                    User.builder()
                            .username("student" + i)
                            .role(UserRole.CLASSMATE)
                            .build()
            ));
        }

        log.info("사용자 시드 데이터 등록 완료: 강사(ID: 1~5), 학생(ID: 6~20)");

        // 3. 다양한 상태와 가격대의 강의(Course) 5개 생성 -> ID: 1 ~ 5
        // Course 1: 실시간 Java 마스터 클래스 (OPEN, 정원 10, 15만원) -> ID: 1
        Course javaCourse = courseRepository.save(Course.builder()
                .creator(creators.get(0)) // creator1
                .title("실시간 Java 마스터 클래스")
                .description("실시간으로 진행하는 초급부터 고급까지의 Java 마스터 코스")
                .price(150000)
                .capacity(10)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .status(CourseStatus.OPEN)
                .build());

        // Course 2: Spring Boot JPA 심화 과정 (OPEN, 정원 2 - 대기열 유도용 풀 코스, 20만원) -> ID: 2
        Course jpaCourse = courseRepository.save(Course.builder()
                .creator(creators.get(0)) // creator1
                .title("Spring Boot JPA 심화 과정")
                .description("실무 프로젝트 성능 개선을 위한 JPA 다각도 심화 학습")
                .price(200000)
                .capacity(2)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(50))
                .status(CourseStatus.OPEN)
                .build());

        // Course 3: 초보자를 위한 HTML/CSS 기초 (DRAFT, 정원 30, 무료) -> ID: 3
        Course htmlCourse = courseRepository.save(Course.builder()
                .creator(creators.get(1)) // creator2
                .title("초보자를 위한 HTML/CSS 기초")
                .description("웹 프론트엔드의 첫 걸음, HTML과 CSS를 정복합니다.")
                .price(0)
                .capacity(30)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .status(CourseStatus.DRAFT)
                .build());

        // Course 4: React & Next.js 프론트엔드 실무 (CLOSED, 정원 20, 30만원) -> ID: 4
        Course reactCourse = courseRepository.save(Course.builder()
                .creator(creators.get(1)) // creator2
                .title("React & Next.js 프론트엔드 실무")
                .description("React 18과 Next.js App Router 기반의 고성능 실무 웹 개발")
                .price(300000)
                .capacity(20)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(5))
                .status(CourseStatus.CLOSED)
                .build());

        // Course 5: 알고리즘 및 자료구조 코딩테스트 (OPEN, 정원 5, 5만원) -> ID: 5
        Course algoCourse = courseRepository.save(Course.builder()
                .creator(creators.get(2)) // creator3
                .title("알고리즘 및 자료구조 코딩테스트")
                .description("대기업/네카라쿠배 코딩테스트 돌파를 위한 정밀 훈련 코스")
                .price(50000)
                .capacity(5)
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(45))
                .status(CourseStatus.OPEN)
                .build());

        log.info("강의 시드 데이터 등록 완료: Java(ID: 1), JPA(ID: 2), HTML(ID: 3), React(ID: 4), 알고리즘(ID: 5)");

        // 4. 수강 신청(Enrollment) 시나리오 데이터 사전 배정
        
        // 시나리오 A: Course 2 (JPA 심화 과정 - 정원 2명) -> 이미 꽉 찬 대기열 구조 연출 (수강신청 ID: 1 ~ 4)
        // - student1 (ID: 6) -> CONFIRMED (결제 완료, 정원 1차지)
        // - student2 (ID: 7) -> PENDING (결제 대기, 정원 2차지)
        // - student3 (ID: 8) -> WAITLISTED (대기 1순위)
        // - student4 (ID: 9) -> WAITLISTED (대기 2순위)
        enrollmentRepository.save(Enrollment.builder()
                .course(jpaCourse)
                .user(students.get(0)) // student1
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now().minusHours(4))
                .confirmedAt(LocalDateTime.now().minusHours(3))
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .course(jpaCourse)
                .user(students.get(1)) // student2
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now().minusHours(3))
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .course(jpaCourse)
                .user(students.get(2)) // student3
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now().minusHours(2))
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .course(jpaCourse)
                .user(students.get(3)) // student4
                .status(EnrollmentStatus.WAITLISTED)
                .enrolledAt(LocalDateTime.now().minusHours(1))
                .build());

        // 시나리오 B: Course 5 (알고리즘 코딩테스트 - 정원 5명) -> 부분 신청 연출 (수강신청 ID: 5 ~ 6)
        // - student5 (ID: 10) -> CONFIRMED
        // - student6 (ID: 11) -> PENDING
        enrollmentRepository.save(Enrollment.builder()
                .course(algoCourse)
                .user(students.get(4)) // student5
                .status(EnrollmentStatus.CONFIRMED)
                .enrolledAt(LocalDateTime.now().minusHours(2))
                .confirmedAt(LocalDateTime.now().minusHours(1))
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .course(algoCourse)
                .user(students.get(5)) // student6
                .status(EnrollmentStatus.PENDING)
                .enrolledAt(LocalDateTime.now().minusHours(1))
                .build());

        // 시나리오 C: Course 1 (Java 마스터 클래스 - 정원 10명) -> 다수 수강생(9명) 사전 매칭하여 페이징 검증 가능하게 함 (수강신청 ID: 7 ~ 15)
        // student7~student15까지 신청
        for (int idx = 6; idx < 15; idx++) {
            EnrollmentStatus status = (idx % 2 == 0) ? EnrollmentStatus.CONFIRMED : EnrollmentStatus.PENDING;
            LocalDateTime time = LocalDateTime.now().minusMinutes(10 * idx);
            enrollmentRepository.save(Enrollment.builder()
                    .course(javaCourse)
                    .user(students.get(idx))
                    .status(status)
                    .enrolledAt(time)
                    .confirmedAt(status == EnrollmentStatus.CONFIRMED ? time.plusMinutes(5) : null)
                    .build());
        }

        log.info("수강 신청(Enrollment) 대용량 시드 시나리오 배정 완료!");
        log.info("=== 풍부한 데이터 환경 준비 완료 ===");
    }
}
