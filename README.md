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

### Docker로 한 번에 실행 (권장)

```bash
docker compose up -d
```

PostgreSQL + Spring Boot 앱이 함께 실행됩니다.  
앱 시작까지 약 30~60초 소요됩니다 (PostgreSQL 준비 완료 후 앱이 자동 기동).

```bash
# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f app
```

### 로컬 개발 환경에서 실행 (IntelliJ / Gradle)

```bash
# 1. PostgreSQL만 먼저 실행
docker compose up -d postgres

# 2. 앱 실행 (터미널 또는 IntelliJ ▶ 버튼)
./gradlew bootRun
```

### 종료

```bash
docker compose down          # 컨테이너 중지 및 제거
docker compose down -v       # 볼륨(DB 데이터)까지 완전 삭제
```

---

서버 최초 기동 시 시드 데이터가 자동 생성됩니다 (이미 데이터가 있으면 건너뜀):

**사용자 (20명)**

| ID | username | role |
|----|----------|------|
| 1 ~ 5 | creator1 ~ creator5 | CREATOR |
| 6 ~ 20 | student1 ~ student15 | CLASSMATE |

**강의 (5개)**

| ID | 제목 | 상태 | 정원 |
|----|------|------|------|
| 1 | 실시간 Java 마스터 클래스 | OPEN | 10 |
| 2 | Spring Boot JPA 심화 과정 | OPEN | 2 (만석 + 대기열) |
| 3 | 초보자를 위한 HTML/CSS 기초 | DRAFT | 30 |
| 4 | React & Next.js 프론트엔드 실무 | CLOSED | 20 |
| 5 | 알고리즘 및 자료구조 코딩테스트 | OPEN | 5 |

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
| 사용자 등록 | `POST /api/users`로 CREATOR/CLASSMATE 계정 직접 생성 가능 (시드 데이터 병행) |

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

### 4. 취소 후 재신청 — DB 유니크 제약 우회

`enrollments` 테이블에는 `(user_id, course_id)` 복합 유니크 제약이 있습니다. 취소 후 재신청 시 새 row를 INSERT하면 제약 위반이 발생하므로, 기존 `CANCELLED` row를 재활성화(`reactivate()`)하는 방식으로 처리합니다.

```
재신청 흐름:
  기존 CANCELLED row 있음 → status=PENDING, enrolledAt=now(), confirmedAt=null, cancelledAt=null 로 갱신
  기존 CANCELLED row 없음 → 새 row INSERT
```

### 5. N+1 쿼리 방지 — 배치 COUNT 쿼리

강의 목록 조회(`getCourses`) 시 각 강의별 신청 인원을 개별 쿼리로 조회하는 대신, 하나의 GROUP BY 쿼리로 한 번에 처리합니다.

```sql
-- 수정 전: N개 강의 → N번 COUNT 쿼리
-- 수정 후: 1번 GROUP BY 쿼리
SELECT e.course_id, COUNT(e.*) FROM enrollments e
WHERE e.course_id IN (?, ?, ...) AND e.status IN ('PENDING', 'CONFIRMED')
GROUP BY e.course_id
```
### 6. 만료 강의 자동 CLOSED — 2-레이어 방어

스케줄러 단독은 서버 다운 중 누락이 발생하므로 두 가지를 함께 사용합니다.

```
레이어 1 (서버 재기동 시) — CourseExpiryCloser
  endDate가 지난 OPEN 강의를 일괄 CLOSED로 전환
  → 서버 중단 기간의 누락분을 재기동 시 즉시 보완

레이어 2 (수강 신청 요청 시) — EnrollmentService.enroll()
  DB 상태와 무관하게 endDate < 오늘이면 COURSE_EXPIRED 반환
  → 극단적인 레이스 컨디션 방어
```

---

## 미구현 / 제약사항

| 항목 | 이유 |
|------|------|
| JWT 인증 | 과제에서 X-User-Id 헤더 방식 명시적으로 허용 |
| 이메일/비밀번호 | User 엔티티가 username, role만 보유 |

---

## AI 활용 범위

본 프로젝트는 **Claude (Anthropic)** 와의 AI 페어 프로그래밍 방식으로 개발되었습니다.

### 본인 기여

| 영역 | 내용 |
|------|------|
| 프로젝트 초기 설정 | Spring Boot 프로젝트 구성, 의존성 선택, Gradle 설정 |
| 기술 스택 결정 | Java 21, Spring Data JPA, PostgreSQL(운영) + H2(테스트), X-User-Id 헤더 인증 방식 채택 |
| 요구사항 해석 | 정원 초과 시 대기열 등록, 취소 후 7일 정책, FIFO 승격 방식 등 비즈니스 정책 결정 |
| 코드 리뷰 | 구현 결과물 검토, 버그 3건 발견(재신청 유니크 위반 / 동시 취소 대기열 누락 / N+1) 및 수정 지시 |
| 성능 개선 식별 | 강의 단건 조회 시 Creator 지연 로딩으로 인한 쿼리 3회 발생 문제를 발견하고 `findByIdWithCreator()` 도입 지시 |
| 시드 데이터 설계 | 대기열 포화 시나리오, 페이지네이션 검증용 다수 신청 등 테스트 시나리오 직접 설계 |
| API 검증 | Swagger UI를 통한 전체 플로우(생성→OPEN→신청→만석→취소→승격) 수동 검증 |
| Docker 환경 | Docker Compose multi-stage 빌드 및 PostgreSQL healthcheck 구성 결정 |

### AI 활용 내용

| 영역 | 내용 |
|------|------|
| 비즈니스 로직 구현 | 엔티티 상태 머신, 서비스 레이어 전체, Repository 쿼리 작성 |
| 테스트 코드 | 단위 테스트(Mockito), MockMvc 통합 테스트, 가상 스레드 동시성 테스트 |
| DB 스키마 | DDL, 유니크 제약, 인덱스 설계 |
| 문서화 | Swagger 설정, Javadoc, README 초안 |

---

## API 목록 및 예시

### 사용자 API

| Method | URL | 설명 | 필요 역할 |
|--------|-----|------|-----------|
| `POST` | `/api/users` | 사용자 등록 (CREATOR 또는 CLASSMATE) | 누구나 |

#### 사용자 등록
```http
POST /api/users
Content-Type: application/json

{ "username": "newStudent", "role": "CLASSMATE" }
```
```json
{ "success": true, "data": { "id": 6, "username": "newStudent", "role": "CLASSMATE" }, "message": null }
```

---

### 강의 API

| Method | URL | 설명 | 필요 역할 |
|--------|-----|------|-----------|
| `POST` | `/api/courses` | 강의 등록 | CREATOR |
| `GET` | `/api/courses` | 강의 목록 (다중 필터 선택) | 누구나 |
| `GET` | `/api/courses/{id}` | 강의 상세 | 누구나 |
| `PATCH` | `/api/courses/{id}` | 강의 수정 (DRAFT 상태만) | CREATOR |
| `PATCH` | `/api/courses/{id}/status` | 상태 변경 (본인 강의만) | CREATOR |
| `GET` | `/api/courses/{id}/enrollments` | 수강생 목록 (본인 강의만) | CREATOR |

#### 강의 목록 조회 (필터 파라미터)

| 파라미터 | 타입 | 설명 | 예시 |
|---------|------|------|------|
| `status` | `DRAFT\|OPEN\|CLOSED` | 강의 상태 필터 | `?status=OPEN` |
| `title` | `String` | 제목 키워드 검색 (대소문자 무관) | `?title=java` |
| `minPrice` | `Integer` | 최소 가격 | `?minPrice=0` |
| `maxPrice` | `Integer` | 최대 가격 | `?maxPrice=50000` |
| `startDate` | `yyyy-MM-dd` | 강의 시작일 이후 | `?startDate=2026-06-01` |
| `endDate` | `yyyy-MM-dd` | 강의 종료일 이전 | `?endDate=2026-12-31` |
| `hasVacancies` | `Boolean` | `true`이면 빈 자리 있는 강의만 | `?hasVacancies=true` |

```http
GET /api/courses?status=OPEN&title=spring&minPrice=0&maxPrice=100000&hasVacancies=true
```

---

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
X-User-Id: 6
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
    "userId": 6,
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
X-User-Id: 6
```

#### 수강 취소
```http
DELETE /api/enrollments/1
X-User-Id: 6
```

---

### 에러 코드 목록

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `COURSE_NOT_FOUND` | 404 | 강의를 찾을 수 없습니다 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다 |
| `ENROLLMENT_NOT_FOUND` | 404 | 수강 신청을 찾을 수 없습니다 |
| `COURSE_NOT_ENROLLABLE` | 400 | 신청 가능한 강의가 아닙니다 |
| `COURSE_EXPIRED` | 400 | 수강 기간이 종료된 강의입니다 |
| `COURSE_NOT_EDITABLE` | 400 | DRAFT 상태의 강의만 수정할 수 있습니다 |
| `CAPACITY_BELOW_ENROLLED` | 400 | 현재 수강 인원보다 정원을 줄일 수 없습니다 |
| `INVALID_STATUS_TRANSITION` | 400 | 강의 상태는 DRAFT→OPEN→CLOSED 순서로만 변경 가능합니다 |
| `INVALID_DATE_RANGE` | 400 | 시작일은 종료일보다 이전이어야 합니다 |
| `CANCEL_PERIOD_EXCEEDED` | 400 | 취소 가능 기간(결제 후 7일)이 지났습니다 |
| `ENROLLMENT_NOT_CANCELLABLE` | 400 | 취소할 수 없는 수강 신청 상태입니다 |
| `ENROLLMENT_NOT_CONFIRMABLE` | 400 | 결제 확정할 수 없는 수강 신청 상태입니다 (기타) |
| `ENROLLMENT_ALREADY_CONFIRMED` | 400 | 이미 결제 완료된 강의입니다 |
| `ENROLLMENT_WAITLISTED_NOT_CONFIRMABLE` | 400 | 대기 중인 상태에서는 결제를 진행할 수 없습니다 |
| `ENROLLMENT_CANCELLED_NOT_CONFIRMABLE` | 400 | 취소된 수강 신청은 결제할 수 없습니다 |
| `ALREADY_ENROLLED` | 409 | 이미 신청한 강의입니다 |
| `USER_ALREADY_EXISTS` | 409 | 이미 존재하는 사용자 이름입니다 |
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
  UNIQUE (user_id, course_id)   -- 중복 신청 방지 (CANCELLED 후 재신청 시 새 INSERT 대신 기존 row reactivate)
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
| `CourseServiceTest` | 단위 | 15 | 강의 생성·수정·조회·상태 변경·고급 필터, 역할 검증, 날짜 검증, 만료 강의 OPEN 차단 |
| `EnrollmentServiceTest` | 단위 | 22 | 신청·확정·취소·대기열·재신청·동시취소·상태별 확정 오류, 역할 검증, 만료 강의 차단 |
| `EnrollmentConcurrencyTest` | 통합 | 1 | 동시 신청 시 정원 초과 방지 (비관적 락) |
| `UserServiceTest` | 단위 | 2 | 사용자 등록, 중복 username 거부 |