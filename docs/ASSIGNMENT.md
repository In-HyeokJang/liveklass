# 과제 A — 수강 신청 시스템

> BE 과제 (BE-A) 요구사항 원문 및 구현 추적

---

## 과제 요약

**유형**: CRUD + 비즈니스 규칙형

**기술 제약**
- Spring Boot (Java 또는 Kotlin) — **Java 21 선택**
- JPA 또는 MyBatis — **Spring Data JPA 선택**
- H2 / MySQL / PostgreSQL — **PostgreSQL (운영) + H2 (테스트) 선택**
- 인증/인가: userId를 헤더로 전달하는 방식 허용 — **X-User-Id 헤더 방식 선택**

---

## 구현 범위

### 필수 구현

#### 1. 강의(Class) 관리

| 요구사항 | 구현 여부 | 파일 |
|---------|---------|------|
| 강의 등록 (제목, 설명, 가격, 정원, 수강기간) | ✅ | `CourseService.createCourse()` |
| 강의 상태: DRAFT → OPEN → CLOSED | ✅ | `Course.transitionTo()` |
| DRAFT: 신청 불가 | ✅ | `EnrollmentService.enroll()` 상태 체크 |
| OPEN: 신청 가능 | ✅ | |
| CLOSED: 신청 불가 | ✅ | |
| 강의 목록 조회 (상태 필터) | ✅ | `GET /api/courses?status=OPEN` |
| 강의 상세 조회 (현재 신청 인원 포함) | ✅ | `GET /api/courses/{id}` |

#### 2. 수강 신청(Enrollment) 관리

| 요구사항 | 구현 여부 | 파일 |
|---------|---------|------|
| 수강 신청 (PENDING 상태) | ✅ | `EnrollmentService.enroll()` |
| 신청 상태: PENDING → CONFIRMED → CANCELLED | ✅ | `Enrollment.confirm()`, `cancel()` |
| 결제 확정 처리 (단순 상태 변경) | ✅ | `PATCH /api/enrollments/{id}/confirm` |
| 수강 취소 | ✅ | `DELETE /api/enrollments/{id}` |
| 내 수강 신청 목록 조회 | ✅ | `GET /api/enrollments/my` |

#### 3. 정원 관리 규칙

| 요구사항 | 구현 여부 | 파일 |
|---------|---------|------|
| 정원 초과 신청 거부 | ✅ | `EnrollmentService.enroll()` 정원 체크 |
| 동시 신청 경합 처리 | ✅ | `CourseRepository.findByIdWithLock()` (비관적 락) |

---

### 선택 구현 (추가 점수)

| 요구사항 | 구현 여부 | 비고 |
|---------|---------|------|
| 취소 가능 기간 제한 (결제 후 7일) | ✅ | `Enrollment.cancel()`, `EnrollmentService.CANCEL_WINDOW_DAYS = 7` |
| 대기열(waitlist) 기능 | ✅ | 정원 초과 시 WAITLISTED 등록, 취소 시 자동 승격 |
| 강의별 수강생 목록 조회 (크리에이터 전용) | ✅ | `GET /api/courses/{id}/enrollments` |
| 신청 내역 페이지네이션 | ✅ | `GET /api/enrollments/my?page=0&size=10` |

---

## 설계 메모 (추후 참고)

### 대기열(Waitlist) 미구현 이유
- `EnrollmentStatus`에 `WAITLISTED` 상태 추가 필요
- 정원 빈자리 발생 시 (취소/거절) WAITLISTED → PENDING 자동 승격 로직 필요
- 순서 보장을 위한 `waitlistOrder` 컬럼 또는 `enrolledAt` 기준 FIFO 처리
- 알림 발송 연동 필요성 → 과제 범위 초과로 생략

### 동시성 전략: 비관적 락 선택 이유
낙관적 락(Optimistic Lock) 대비:
- `@Version` 충돌 시 재시도 폭풍 가능 (마지막 자리 경합처럼 충돌 확률이 높은 상황)
- 비관적 락 = 대기 있지만 재시도 없음 → 정원 초과 방지에 더 안정적

---

## 제출물 체크리스트

- [x] Spring Boot REST API 구현
- [x] API 명세 (README.md `## API 목록 및 예시`)
- [x] DB 스키마 (`db/init.sql`, README.md `## 데이터 모델 설명`)
- [x] README 필수 항목 모두 포함
- [x] 테스트 코드 (단위 + 통합 + 동시성)
- [x] Docker Compose 실행 환경