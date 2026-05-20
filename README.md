# LiveKlass — 수강 신청 시스템

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청·결제 확정·취소를 할 수 있는 REST API 서버입니다.

주요 특징:
- 강의 상태 머신: `DRAFT → OPEN → CLOSED`
- 수강 신청 상태 머신: `PENDING → CONFIRMED → CANCELLED`, 대기열: `WAITLISTED → PENDING`
- 동시 신청 정원 초과 방지 (비관적 락, SELECT FOR UPDATE)
- 정원 초과 시 자동 대기열 등록, 취소 시 첫 번째 대기자 자동 승격
- 결제 확정 후 7일 이내 취소 정책
- 역할 기반 접근 제어 (CREATOR / CLASSMATE)

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| ORM | Spring Data JPA (Hibernate) |
| DB (운영) | PostgreSQL 15 |
| DB (테스트) | H2 (in-memory, PostgreSQL 모드) |
| 빌드 | Gradle |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 기타 | Lombok, Spring Validation, Docker Compose |

---

## 실행 방법

### 사전 준비: Docker로 PostgreSQL 실행

```bash
docker compose up -d
```

`postgres:15` 컨테이너가 `localhost:5432`로 실행됩니다.  
DB/계정 설정은 `docker-compose.yml` 참고 (기본값: postgres/postgres/liveklass).

### 애플리케이션 실행

```bash
./gradlew bootRun
```

서버 기동 시 시드 데이터가 자동 생성됩니다:

| ID | username | role |
|----|----------|------|
| 1 | creator1 | CREATOR |
| 2 | creator2 | CREATOR |
| 3 | student1 | CLASSMATE |
| 4 | student2 | CLASSMATE |
| 5 | student3 | CLASSMATE |

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 요구사항 해석 및 가정

| 항목 | 해석 및 가정 |
|------|-------------|
| 인증 | `X-User-Id` 헤더로 userId 전달. JWT 미구현 (과제 허용) |
| 역할 검증 | CREATOR만 강의 생성·상태변경 가능. CLASSMATE만 수강 신청 가능 |
| 결제 | 외부 연동 없이 `PATCH /api/enrollments/{id}/confirm` 호출로 상태를 CONFIRMED로 변경 |
| 취소 기간 | CONFIRMED 후 7일 이내만 취소 가능. PENDING은 기간 제한 없이 취소 가능 |
| 정원 계산 | PENDING + CONFIRMED 상태 수강 신청 수를 기준으로 정원 체크 (CANCELLED 제외) |
| 동시성 | 마지막 자리 경합 시 비관적 락으로 정원 초과 방지 |
| 사용자 등록 | API 미구현, 시드 데이터로 대체 |

---

## 설계 결정과 이유

### 1. 동시성 제어 — 비관적 락 (Pessimistic Write Lock)

마지막 한 자리를 두고 여러 사람이 동시에 신청할 때 정원을 초과하지 않도록 `SELECT FOR UPDATE`를 사용합니다.

```java
// CourseRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Course c WHERE c.id = :id")
Optional<Course> findByIdWithLock(@Param("id") Long id);
```

낙관적 락(Optimistic Lock)도 고려했으나, 마지막 자리 경합처럼 충돌 가능성이 높은 경우 재시도 폭풍이 발생할 수 있어 비관적 락을 선택했습니다.

### 2. 상태 전이 검증 — 엔티티 내부 캡슐화

```
Course:     DRAFT → OPEN → CLOSED  (역방향 불가)
Enrollment: PENDING → CONFIRMED    (CONFIRMED는 PENDING에서만)
            PENDING → CANCELLED    (기간 제한 없음)
            CONFIRMED → CANCELLED  (확정 후 7일 이내만)
```

상태 전이 규칙은 도메인 지식이므로 엔티티(`Course.transitionTo`, `Enrollment.confirm/cancel`) 내부에서 검증합니다. 서비스가 규칙을 알 필요 없이 메서드를 호출만 하면 됩니다.

### 3. 패키지 구조 — 도메인 중심

```
com.samintech.liveklass
├── common/       (ApiResponse, ErrorCode, GlobalExceptionHandler)
├── course/       (Course, CourseService, CourseController, ...)
├── enrollment/   (Enrollment, EnrollmentService, EnrollmentController, ...)
└── user/         (User, UserRepository, UserRole)
```

계층별 분리(controller/service/repository)보다 도메인별 응집도가 높아 기능 수정 시 파일 탐색 범위가 좁습니다.

### 4. N+1 쿼리 방지 — 배치 COUNT 쿼리

강의 목록 조회(`getCourses`) 시 각 강의별 신청 인원을 개별 쿼리로 조회하는 대신, 하나의 GROUP BY 쿼리로 한 번에 처리합니다.

```sql
-- 수정 전: N개 강의 → N번 COUNT 쿼리
-- 수정 후: 1번 GROUP BY 쿼리
SELECT e.course_id, COUNT(e.*) FROM enrollments e
WHERE e.course_id IN (?, ?, ...) AND e.status IN ('PENDING', 'CONFIRMED')
GROUP BY e.course_id
```

---

## 미구현 / 제약사항

| 항목 | 이유 |
|------|------|
| 사용자 등록 API | 시드 데이터(DataInitializer)로 대체 |
| JWT 인증 | 과제에서 X-User-Id 헤더 방식 명시적으로 허용 |
| 이메일/비밀번호 | User 엔티티가 username, role만 보유 |

---

## AI 활용 범위

Claude (claude-sonnet-4-6)를 활용하여 전체 프로젝트 골격 생성 및 코드 리뷰를 진행했습니다.

**AI가 생성한 주요 결과물**
- 엔티티, 서비스, 컨트롤러, 리포지토리 전체 구조
- 테스트 코드 (단위 + 통합 + 동시성)
- 오류 처리 공통 구조 (GlobalExceptionHandler, ErrorCode)

**직접 검토·결정한 사항**
- 비관적 락 vs 낙관적 락 트레이드오프 이해 후 비관적 락 선택
- 상태 전이 검증 위치 (서비스 vs 엔티티) → 엔티티 내부 결정
- 취소 가능 기간 규칙 (PENDING vs CONFIRMED 분기 처리)
- UserRole 기반 접근 제어 범위 결정
- 테스트 케이스 경계값 및 시나리오 검토

---

## API 목록 및 예시

### 강의 API

| Method | URL | 설명 | 필요 역할 |
|--------|-----|------|-----------|
| `POST` | `/api/courses` | 강의 등록 | CREATOR |
| `GET` | `/api/courses?status=OPEN` | 강의 목록 (상태 필터 선택) | 누구나 |
| `GET` | `/api/courses/{id}` | 강의 상세 | 누구나 |
| `PATCH` | `/api/courses/{id}/status` | 상태 변경 (본인 강의만) | CREATOR |
| `GET` | `/api/courses/{id}/enrollments` | 수강생 목록 (본인 강의만) | CREATOR |

#### 강의 등록
```http
POST /api/courses
X-User-Id: 1
Content-Type: application/json

{
  "title": "Spring Boot 마스터",
  "description": "실전 Spring Boot",
  "price": 99000,
  "capacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Spring Boot 마스터",
    "description": "실전 Spring Boot",
    "price": 99000,
    "capacity": 30,
    "enrolledCount": 0,
    "startDate": "2026-06-01",
    "endDate": "2026-08-31",
    "status": "DRAFT"
  },
  "message": null
}
```

#### 상태 변경 (DRAFT → OPEN)
```http
PATCH /api/courses/1/status
X-User-Id: 1
Content-Type: application/json

{ "status": "OPEN" }
```

#### 에러 응답 예시
```json
{
  "success": false,
  "data": null,
  "message": "강의 정원이 초과되었습니다"
}
```

---

### 수강 신청 API

| Method | URL | 설명 | 필요 역할 |
|--------|-----|------|-----------|
| `POST` | `/api/enrollments` | 수강 신청 | CLASSMATE |
| `GET` | `/api/enrollments/my?page=0&size=10` | 내 신청 목록 (페이지네이션) | CLASSMATE |
| `PATCH` | `/api/enrollments/{id}/confirm` | 결제 확정 | CLASSMATE |
| `DELETE` | `/api/enrollments/{id}` | 수강 취소 | CLASSMATE |

#### 수강 신청
```http
POST /api/enrollments
X-User-Id: 3
Content-Type: application/json

{ "courseId": 1 }
```
```json
{
  "success": true,
  "data": {
    "id": 1,
    "courseId": 1,
    "courseTitle": "Spring Boot 마스터",
    "userId": 3,
    "username": "student1",
    "status": "PENDING",
    "enrolledAt": "2026-05-20T10:00:00",
    "confirmedAt": null,
    "cancelledAt": null
  },
  "message": null
}
```

#### 결제 확정
```http
PATCH /api/enrollments/1/confirm
X-User-Id: 3
```

#### 수강 취소
```http
DELETE /api/enrollments/1
X-User-Id: 3
```

---

### 에러 코드 목록

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `COURSE_NOT_FOUND` | 404 | 강의를 찾을 수 없습니다 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다 |
| `ENROLLMENT_NOT_FOUND` | 404 | 수강 신청을 찾을 수 없습니다 |
| `COURSE_NOT_ENROLLABLE` | 400 | 신청 가능한 강의가 아닙니다 (OPEN 상태 아님) |
| `INVALID_STATUS_TRANSITION` | 400 | 유효하지 않은 상태 변경입니다 |
| `INVALID_DATE_RANGE` | 400 | 시작일은 종료일보다 이전이어야 합니다 |
| `CANCEL_PERIOD_EXCEEDED` | 400 | 취소 가능 기간(결제 후 7일)이 지났습니다 |
| `ENROLLMENT_NOT_CANCELLABLE` | 400 | 취소할 수 없는 수강 신청 상태입니다 |
| `ENROLLMENT_NOT_CONFIRMABLE` | 400 | 결제 확정할 수 없는 수강 신청 상태입니다 |
| `COURSE_FULL` | 409 | 강의 정원이 초과되었습니다 (현재 미사용 — 정원 초과 시 WAITLISTED로 등록) |
| `ALREADY_ENROLLED` | 409 | 이미 신청한 강의입니다 |
| `NOT_COURSE_CREATOR` | 403 | 강의 개설자만 접근할 수 있습니다 |
| `NOT_ENROLLMENT_OWNER` | 403 | 본인의 수강 신청만 처리할 수 있습니다 |
| `UNAUTHORIZED_ROLE` | 403 | 해당 작업을 수행할 권한이 없습니다 |

---

## 데이터 모델 설명

```
users
  id (PK, BIGSERIAL)
  username (VARCHAR, UNIQUE, NOT NULL)
  role (VARCHAR: CREATOR | CLASSMATE, NOT NULL)

courses
  id (PK, BIGSERIAL)
  creator_id (FK → users.id, NOT NULL)
  title (VARCHAR, NOT NULL)
  description (TEXT)
  price (INTEGER, NOT NULL)
  capacity (INTEGER, NOT NULL)
  start_date (DATE, NOT NULL)
  end_date (DATE, NOT NULL)
  status (VARCHAR: DRAFT | OPEN | CLOSED, NOT NULL)
  created_at (TIMESTAMP)
  updated_at (TIMESTAMP)

enrollments
  id (PK, BIGSERIAL)
  user_id (FK → users.id, NOT NULL)
  course_id (FK → courses.id, NOT NULL)
  status (VARCHAR: PENDING | CONFIRMED | CANCELLED | WAITLISTED, NOT NULL)
  enrolled_at (TIMESTAMP, NOT NULL)   -- 대기열 진입 시간 기준 FIFO 정렬
  confirmed_at (TIMESTAMP)
  cancelled_at (TIMESTAMP)
  UNIQUE (user_id, course_id)   -- 중복 신청 방지 (CANCELLED 후 재신청 시 DB 제약 주의)
```

**대기열 정책**
- 정원이 꽉 찬 OPEN 강의에 신청 시 → `WAITLISTED` 상태로 등록 (성공 응답)
- PENDING 또는 CONFIRMED 수강생이 취소 시 → `enrolled_at` 기준 가장 오래된 WAITLISTED 대기자를 `PENDING`으로 자동 승격
- WAITLISTED는 정원(capacity) 카운트에 포함되지 않음

### ERD

```
users (1) ──< courses (1) ──< enrollments >── (1) users
               [creator_id]      [course_id]       [user_id]
```

---

## 테스트 실행 방법

```bash
# 전체 테스트 (H2 인메모리 DB 사용, PostgreSQL 불필요)
./gradlew test

# 테스트 리포트 확인
# Windows: start build/reports/tests/test/index.html
# Mac/Linux: open build/reports/tests/test/index.html
```

| 테스트 클래스 | 종류 | 케이스 수 | 설명 |
|---|---|---|---|
| `LiveKlassApplicationTests` | 통합 | 1 | 애플리케이션 컨텍스트 로드 |
| `CourseControllerTest` | MockMvc | 6 | API 요청/응답 형식, 유효성 검증 |
| `CourseServiceTest` | 단위 | 10 | 강의 생성·조회·상태 변경, 역할 검증, 날짜 검증 |
| `EnrollmentServiceTest` | 단위 | 13 | 신청·확정·취소·대기열 비즈니스 규칙, 역할 검증 |
| `EnrollmentConcurrencyTest` | 통합 | 1 | 동시 신청 시 정원 초과 방지 (비관적 락) |