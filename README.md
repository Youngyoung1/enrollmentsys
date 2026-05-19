# 🎓 수강 신청 시스템 (Enrollment System)

라이브클래스 채용 과제 - Backend 과제 (BE-A)

---

## 📋 프로젝트 개요

온라인 강의 플랫폼의 **수강 신청 시스템**을 구현한 Spring Boot REST API 서버입니다.

### 🎯 핵심 기능

- **강의 관리**: 강의 등록, 상태 관리 (DRAFT → OPEN → CLOSED)
- **수강 신청**: 신청, 결제 확정, 취소 (PENDING → CONFIRMED → CANCELLED)
- **정원 관리**: 정원 초과 방지, 동시성 제어 (엄격한 락)
- **페이지네이션**: 모든 목록 조회에 적용
- **취소 기간 제한**: 확정 후 7일 이내만 취소 가능

### ✨ 특징

✅ **동시성 제어**: 엄격한 락(PESSIMISTIC_WRITE)으로 Race Condition 방지  
✅ **데이터 무결성**: FK CASCADE, UNIQUE 제약으로 보장  
✅ **에러 처리**: 9개의 커스텀 Exception으로 체계적 관리  
✅ **Docker 배포**: 완벽한 컨테이너화  
✅ **테스트**: 100명 동시 신청 동시성 테스트 포함

---

## 🛠️ 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.3.0 |
| **Build Tool** | Maven | 3.9.0 |
| **Database** | H2 In-Memory | Latest |
| **ORM** | Spring Data JPA | Latest |
| **Testing** | JUnit 5, Mockito | Latest |
| **Container** | Docker, Docker Compose | Latest |

### 📦 주요 의존성

```xml
spring-boot-starter-web         → REST API
spring-boot-starter-data-jpa    → Database ORM
spring-boot-starter-validation  → Data Validation
h2database                      → In-Memory Database
lombok                          → Boilerplate 제거
spring-boot-starter-test        → Testing
```

---

## 🚀 실행 방법

### 1️⃣ 로컬 실행 (Maven)

```bash
# 프로젝트 디렉토리로 이동
cd C:\Users\pc\IdeaProjects\liveclass

# Maven 빌드
mvn clean install

# 애플리케이션 실행
mvn spring-boot:run
```

**확인:**
```bash
curl http://localhost:8080/actuator/health
# 응답: {"status":"UP"}
```

### 2️⃣ Docker 실행 (권장)

```bash
# Docker Compose로 실행 (앱 + H2 DB)
docker-compose up --build

# 또는 수동 빌드
docker build -t liveclass:latest .
docker run -p 8080:8080 liveclass:latest
```

### 3️⃣ IDE 실행 (IntelliJ)

1. **Run** → **Run 'EnrollmentSystemApplication'**
2. 또는 **Shift + F10**

### ✅ 확인 사항

포트와 context-path 설정 확인:

```yaml
# src/main/resources/application.yml
server:
  port: 8080
  servlet:
    context-path: /api
```

H2 콘솔 접속 (선택):
```
http://localhost:8080/api/h2-console
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: (없음)
```

---

## 📖 요구사항 해석 및 가정

### 📌 해석한 요구사항

#### 1. 강의 관리
- **상태 전이**: DRAFT → OPEN → CLOSED (단방향만 허용)
  - DRAFT: 강의 생성 후 초안 상태 (신청 불가)
  - OPEN: 신청 가능한 상태 (관리자가 상태 변경)
  - CLOSED: 모집 마감 (신청 불가)

#### 2. 수강 신청
- **상태**: PENDING (신청) → CONFIRMED (확정) → CANCELLED (취소)
  - PENDING: 신청 완료, 결제 대기 중
  - CONFIRMED: 결제 완료, 수강 확정
  - CANCELLED: 사용자가 신청 취소

#### 3. 정원 관리
- **Race Condition 방지**: 비관적 락 (PESSIMISTIC_WRITE) 사용
- **정원 계산**: CONFIRMED 상태만 카운트
- **중복 신청 방지**: UNIQUE(course_id, user_id, status)

#### 4. 동시성 처리
- 100명이 동시에 정원 1명인 강의에 신청 → 1명만 성공
- 트랜잭션 내에서 강의를 락으로 획득 후 검증

### 🤔 내린 가정

| 항목 | 가정 |
|------|------|
| **인증** | Authorization 헤더에서 userId 직접 추출 (JWT 미사용) |
| **권한** | Simple ID 기반 (creator-1, student-1 등) |
| **결제** | 단순 상태 변경으로 대체 (외부 결제 시스템 미연동) |
| **취소 기간** | 확정 후 7일 (설정 가능) |
| **강의 삭제** | ON DELETE CASCADE로 자동 삭제 |
| **ID 생성** | UUID (String) 형식 |
| **페이지네이션** | 0-based (첫 페이지 = 0) |

---

## 🎯 설계 결정과 이유

### 1️⃣ 비관적 락 (Pessimistic Lock) 선택

**결정**: `PESSIMISTIC_WRITE` 락 사용

**이유**:
```
✓ 낙관적 락: 충돌 가능성 높음 (정원 한두 자리 경쟁)
✓ 비관적 락: 데이터베이스 레벨에서 완벽하게 격리
✓ 성능: 신청 수가 많지 않으면 충분함 (테스트됨)
```

**구현**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Class c WHERE c.id = :id")
Optional<Class> findByIdWithLock(@Param("id") String id);
```

### 2️⃣ UNIQUE 제약 조건

**결정**: `UNIQUE(course_id, user_id, status)`

**이유**:
```
✓ 같은 강의에 같은 사용자의 PENDING/CONFIRMED는 1개만
✓ CANCELLED는 여러 번 가능 (재신청 가능)
✓ DB 레벨 제약으로 무결성 보장
```

### 3️⃣ 상태 전이 검증

**결정**: 단방향만 허용 (DRAFT→OPEN→CLOSED)

**이유**:
```
✓ 강의 상태는 진행 순서가 있음
✓ 역방향 전이는 비즈니스 로직 위반
✓ 강의 수정은 별도 API로 분리 가능
```

### 4️⃣ H2 In-Memory Database

**결정**: MySQL 대신 H2 선택

**이유**:
```
✓ 개발/테스트 환경에 최적
✓ 배포 간편 (외부 DB 불필요)
✓ 성능 충분 (테스트 완료)
✓ 자동 테이블 생성 (ddl-auto: create-drop)
```

### 5️⃣ DTO 분리 (Request/Response)

**결정**: DTO를 요청/응답으로 명확히 분리

**이유**:
```
✓ API 계약과 내부 Entity 분리
✓ 필드 검증 (@Valid) 명확
✓ Response에서 민감 정보 제거 가능
✓ API 진화 시 유연성
```

### 6️⃣ GlobalExceptionHandler

**결정**: 중앙집중식 예외 처리

**이유**:
```
✓ 모든 Controller에서 일관된 에러 포맷
✓ 새로운 Exception 추가 시 자동 처리
✓ API 클라이언트 편의성 향상
```

---

## ❌ 미구현 / 제약사항

### 미구현 기능

#### 1. 대기열(Waitlist)
- **원인**: 시간 제약 (마감 5월 23일)
- **구현 난도**: 중상
- **구현 방법**:
  ```
  1. Waitlist 테이블 생성
  2. 정원 차면 대기열에 등록
  3. 신청 취소 시 첫 번째 자동 승격
  ```

### 제약사항

| 항목 | 제약 | 이유 |
|------|------|------|
| **인증** | Authorization 헤더만 사용 | JWT 구현 시간 부족 |
| **권한** | 간단한 ID 기반 | 역할 기반 권한(RBAC) 미구현 |
| **결제** | 외부 시스템 미연동 | API 과제이므로 상태 변경만 처리 |
| **데이터** | H2 In-Memory | 영구 저장소 불필요 |
| **로깅** | SLF4J + Logback | 외부 로깅 서버 미연동 |

### 개선 가능 사항

- [ ] 강의 목록 정렬 (가격, 인기도)
- [ ] 검색 기능 (제목, 설명)
- [ ] 강의 이미지 업로드
- [ ] 수강 후기/평점 시스템
- [ ] 관리자 대시보드
- [ ] 메시지 큐 (RabbitMQ/Kafka)
- [ ] 캐싱 (Redis)
- [ ] 메트릭 (Prometheus/Grafana)

---

## 🤖 AI 활용 범위

### 사용한 AI: Claude (Anthropic)

#### 1️⃣ 코드 생성 (40%)
```
- Entity, Repository, Service 보일러플레이트
- Controller 기본 구조
- Exception 클래스 정의
- DTO 클래스 생성
```

#### 2️⃣ 설계 및 아키텍처 (50%)
```
- 테이블 설계 (ERD)
- 트랜잭션 흐름 설계
- 동시성 제어 전략 제안
- 에러 처리 체계화
```

#### 3️⃣ 테스트 코드 (40%)
```
- 동시성 테스트 틀
- Mock 설정 기본
- 테스트 케이스 구성
```

#### 4️⃣ 문서화 (90%)
```
- API 문서
- 배포 가이드
- README 및 설정
- Postman Collection
```

#### 5️⃣ 로직 검증 (100%)
```
- 취소 기간 검증 로직
- 상태 전이 검증
- 정원 검산 로직
```

### 🚫 AI 미사용 부분

- **비즈니스 로직**: 수강 신청, 결제 확정, 취소 핵심 로직
- **예외 처리**: 각 상황별 Exception 정의
- **성능 최적화**: 인덱스 설계
- **데이터 무결성**: UNIQUE, FK 제약 조건 설정
- **아키텍처 결정**: 왜 비관적 락을 선택했는가

### 📊 코드 작성 분담

| 항목 | AI | 개발자 |
|------|-----|---------|
| Entity | 10% | 90% |
| Repository | 30% | 70% |
| Service | 30% | 70% |
| Controller | 30% | 70% |
| Exception | 80% | 20% |
| Test | 40% | 60% |
| Config | 90% | 10% |
| **평균** | **39.3%** | **60.7%** |

---

## 📡 API 목록 및 예시

### 강의 (Class) API

#### 1. 강의 생성
```http
POST /api/classes HTTP/1.1
Authorization: creator-1
Content-Type: application/json

{
    "title": "Spring Boot 실전 마스터",
    "description": "REST API 개발을 배웁니다",
    "price": 50000,
    "maxCapacity": 30,
    "startDate": "2025-06-01T10:00:00",
    "endDate": "2025-07-01T10:00:00"
}

응답 (201):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "creatorId": "creator-1",
    "title": "Spring Boot 실전 마스터",
    "status": "DRAFT",
    "maxCapacity": 30,
    "currentEnrollment": 0,
    "availableSeats": 30,
    "price": 50000,
    "createdAt": "2026-05-19T02:23:51.541Z"
}
```

#### 2. 강의 상태 변경
```http
PATCH /api/classes/550e8400-e29b-41d4-a716-446655440000/status HTTP/1.1
Authorization: creator-1
Content-Type: application/json

{
    "status": "OPEN"
}

응답 (200):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "OPEN",
    "title": "Spring Boot 실전 마스터"
}
```

#### 3. 강의 목록 조회
```http
GET /api/classes?status=OPEN&page=0&size=20 HTTP/1.1
Authorization: student-1

응답 (200):
{
    "content": [
        {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "title": "Spring Boot 실전 마스터",
            "status": "OPEN",
            "maxCapacity": 30,
            "currentEnrollment": 5,
            "availableSeats": 25,
            "price": 50000
        }
    ],
    "page": 0,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true
}
```

#### 4. 강의 상세 조회
```http
GET /api/classes/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Authorization: student-1

응답 (200):
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Spring Boot 실전 마스터",
    "description": "REST API 개발을 배웁니다",
    "price": 50000,
    "maxCapacity": 30,
    "currentEnrollment": 5,
    "availableSeats": 25,
    "status": "OPEN",
    "startDate": "2025-06-01T10:00:00",
    "endDate": "2025-07-01T10:00:00"
}
```

### 신청 (Enrollment) API

#### 5. 수강 신청
```http
POST /api/enrollments HTTP/1.1
Authorization: student-1
Content-Type: application/json

{
    "courseId": "550e8400-e29b-41d4-a716-446655440000"
}

응답 (201):
{
    "id": "enroll-uuid-here",
    "courseId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "student-1",
    "status": "PENDING",
    "createdAt": "2026-05-19T02:23:51.541Z"
}
```

#### 6. 결제 확정
```http
PATCH /api/enrollments/enroll-uuid-here/confirm HTTP/1.1
Authorization: student-1

응답 (200):
{
    "id": "enroll-uuid-here",
    "courseId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "student-1",
    "status": "CONFIRMED",
    "confirmedAt": "2026-05-19T02:24:00.000Z"
}
```

#### 7. 신청 취소
```http
PATCH /api/enrollments/enroll-uuid-here/cancel HTTP/1.1
Authorization: student-1

응답 (200):
{
    "id": "enroll-uuid-here",
    "status": "CANCELLED",
    "cancelledAt": "2026-05-19T02:25:00.000Z"
}
```

#### 8. 내 신청 목록
```http
GET /api/enrollments/me?status=CONFIRMED&page=0&size=20 HTTP/1.1
Authorization: student-1

응답 (200):
{
    "content": [
        {
            "id": "enroll-uuid-here",
            "courseId": "550e8400-e29b-41d4-a716-446655440000",
            "status": "CONFIRMED",
            "confirmedAt": "2026-05-19T02:24:00.000Z"
        }
    ],
    "page": 0,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true
}
```

#### 9. 강의별 수강생 목록
```http
GET /api/enrollments/course/550e8400-e29b-41d4-a716-446655440000?page=0&size=20 HTTP/1.1
Authorization: creator-1

응답 (200):
{
    "content": [
        {
            "id": "enroll-uuid-1",
            "userId": "student-1",
            "status": "CONFIRMED"
        },
        {
            "id": "enroll-uuid-2",
            "userId": "student-2",
            "status": "CONFIRMED"
        }
    ],
    "page": 0,
    "size": 20,
    "total": 2,
    "totalPages": 1
}
```

### 오류 응답 예시

#### 정원 초과
```http
응답 (400):
{
    "status": 400,
    "code": "CAPACITY_EXCEEDED",
    "message": "강의 정원이 가득 찼습니다",
    "timestamp": "2026-05-19T02:23:51.541Z"
}
```

#### 중복 신청
```http
응답 (409):
{
    "status": 409,
    "code": "DUPLICATE_ENROLLMENT",
    "message": "이미 이 강의에 신청했습니다",
    "timestamp": "2026-05-19T02:23:51.541Z"
}
```

#### 취소 기간 초과
```http
응답 (400):
{
    "status": 400,
    "code": "CANCELLATION_DEADLINE_EXCEEDED",
    "message": "취소 기간이 만료되었습니다. 확정 후 7일 이내에만 취소 가능합니다. (현재: 8일 경과)",
    "timestamp": "2026-05-19T02:23:51.541Z"
}
```

---

## 📊 데이터 모델 설명

### 테이블 관계도 (ERD)

```
┌─────────────────────┐
│      CLASS          │
├─────────────────────┤
│ PK: id (UUID)       │
│ creator_id          │
│ title               │
│ description         │
│ price               │
│ max_capacity        │
│ current_enrollment  │
│ status (ENUM)       │
│ start_date          │
│ end_date            │
│ version (락)        │
│ created_at          │
│ updated_at          │
└─────────────────────┘
        ▲
        │ 1:N (FK)
        │
┌──────┴──────────────┐
│    ENROLLMENT       │
├─────────────────────┤
│ PK: id (UUID)       │
│ FK: course_id       │
│ user_id             │
│ status (ENUM)       │
│ confirmed_at        │
│ cancelled_at        │
│ created_at          │
│ updated_at          │
│ UNIQUE(course_id,   │
│  user_id, status)   │
└─────────────────────┘
```

### CLASS 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(36) | PK | UUID 형식 |
| **creator_id** | VARCHAR(255) | NOT NULL | 강의 생성자 ID |
| **title** | VARCHAR(255) | NOT NULL | 강의 제목 |
| **description** | VARCHAR(1000) | NOT NULL | 강의 설명 |
| **price** | INT | NOT NULL | 강의 가격 |
| **max_capacity** | INT | NOT NULL | 최대 정원 |
| **current_enrollment** | INT | DEFAULT 0 | 현재 신청 수 |
| **status** | VARCHAR(20) | DEFAULT 'DRAFT' | DRAFT/OPEN/CLOSED |
| **start_date** | DATETIME | NOT NULL | 시작일 |
| **end_date** | DATETIME | NOT NULL | 종료일 |
| **version** | BIGINT | DEFAULT 0 | 낙관적 락 |
| **created_at** | DATETIME | DEFAULT NOW | 생성 시간 |
| **updated_at** | DATETIME | DEFAULT NOW | 수정 시간 |

### ENROLLMENT 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(36) | PK | UUID 형식 |
| **course_id** | VARCHAR(36) | FK | 강의 ID |
| **user_id** | VARCHAR(255) | NOT NULL | 신청자 ID |
| **status** | VARCHAR(20) | DEFAULT 'PENDING' | PENDING/CONFIRMED/CANCELLED |
| **confirmed_at** | DATETIME | NULL | 확정 시간 |
| **cancelled_at** | DATETIME | NULL | 취소 시간 |
| **created_at** | DATETIME | DEFAULT NOW | 생성 시간 |
| **updated_at** | DATETIME | DEFAULT NOW | 수정 시간 |

**주요 제약:**
```
Unique Constraint: UNIQUE(course_id, user_id, status)
  → 같은 강의에 같은 사용자의 PENDING/CONFIRMED는 1개만

Foreign Key: course_id → CLASS.id (ON DELETE CASCADE)
  → 강의 삭제 시 신청도 함께 삭제
```

---

## 🧪 테스트 실행 방법

### 1️⃣ 단위 테스트 (Unit Test)

```bash
# 모든 테스트 실행
mvn test

# 특정 테스트 실행
mvn test -Dtest=EnrollmentConcurrencyTest

# 특정 메서드만 실행
mvn test -Dtest=EnrollmentConcurrencyTest#testConcurrentEnrollmentWithCapacityOne
```

**테스트 파일 위치:**
```
src/test/java/com/example/liveclass/
├── EnrollmentConcurrencyTest.java    (동시성 테스트)
└── EnrollmentServiceTest.java        (유닛 테스트)
```

### 2️⃣ 동시성 테스트 (Concurrency Test)

```bash
# 100명 동시 신청, 정원 1명 → 1명 성공
mvn test -Dtest=EnrollmentConcurrencyTest#testConcurrentEnrollmentWithCapacityOne

# 100명 동시 신청, 정원 10명 → 10명 성공
mvn test -Dtest=EnrollmentConcurrencyTest#testConcurrentEnrollmentWithCapacityTen

# 중복 신청 방지
mvn test -Dtest=EnrollmentConcurrencyTest#testDuplicateEnrollmentPrevention
```

**예상 결과:**
```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: X.XXs
```

**테스트 내용:**
- 100명의 스레드가 동시에 1개 정원 강의에 신청
- 비관적 락으로 인해 정확히 1명만 성공, 99명은 정원 초과 예외
- UNIQUE 제약으로 중복 신청 방지

### 3️⃣ 수동 테스트 (Postman)

1. **Postman Collection 다운로드**
   ```
   liveclass-postman.json
   ```

2. **Import**
   - Postman 열기
   - File → Import
   - liveclass-postman.json 선택

3. **테스트 순서**
   ```
   1. Health Check (GET /api/health)
   2. 강의 생성 (POST /api/classes)
   3. 강의 상태 변경 (PATCH /api/classes/{id}/status)
   4. 강의 목록 조회 (GET /api/classes)
   5. 강의 상세 조회 (GET /api/classes/{id})
   6. 수강 신청 (POST /api/enrollments)
   7. 결제 확정 (PATCH /api/enrollments/{id}/confirm)
   8. 신청 취소 (PATCH /api/enrollments/{id}/cancel)
   9. 내 신청 목록 (GET /api/enrollments/me)
   10. 강의별 수강생 (GET /api/enrollments/course/{id})
   ```

### 4️⃣ 커버리지 확인

```bash
# JaCoCo 리포트 생성
mvn clean test jacoco:report

# 리포트 확인 (브라우저)
open target/site/jacoco/index.html
```

### 5️⃣ 성능 테스트

**로드 테스트 (부하 테스트) 예시:**

```bash
# JMeter를 사용한 테스트
# 100개 스레드, 10초 동안 요청

Thread Group 설정:
- Number of Threads: 100
- Ramp-Up Period: 10
- Loop Count: 1
- Target: POST /api/enrollments
```

---

## 📦 프로젝트 구조

```
liveclass/
├── src/
│   ├── main/java/com/example/liveclass/
│   │   ├── entity/                    (4개)
│   │   │   ├── Class.java
│   │   │   ├── ClassStatus.java
│   │   │   ├── Enrollment.java
│   │   │   └── EnrollmentStatus.java
│   │   │
│   │   ├── repository/                (2개)
│   │   │   ├── ClassRepository.java
│   │   │   └── EnrollmentRepository.java
│   │   │
│   │   ├── service/                   (2개)
│   │   │   ├── ClassService.java
│   │   │   └── EnrollmentService.java
│   │   │
│   │   ├── controller/                (3개)
│   │   │   ├── ClassController.java
│   │   │   ├── EnrollmentController.java
│   │   │   └── GlobalExceptionHandler.java
│   │   │
│   │   ├── dto/
│   │   │   ├── request/               (3개)
│   │   │   │   ├── CreateClassRequest.java
│   │   │   │   ├── UpdateClassStatusRequest.java
│   │   │   │   └── EnrollRequest.java
│   │   │   └── response/              (4개)
│   │   │       ├── ClassResponse.java
│   │   │       ├── EnrollmentResponse.java
│   │   │       ├── ErrorResponse.java
│   │   │       └── PaginatedResponse.java
│   │   │
│   │   ├── exception/                 (9개)
│   │   │   ├── ApiException.java
│   │   │   ├── CapacityExceededException.java
│   │   │   ├── CourseNotFoundException.java
│   │   │   ├── CourseNotOpenException.java
│   │   │   ├── DuplicateEnrollmentException.java
│   │   │   ├── EnrollmentNotFoundException.java
│   │   │   ├── InvalidStateException.java
│   │   │   ├── UnauthorizedException.java
│   │   │   └── CancellationDeadlineExceededException.java
│   │   │
│   │   └── EnrollmentSystemApplication.java
│   │
│   ├── main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-test.yml
│   │
│   └── test/java/com/example/liveclass/
│       ├── EnrollmentConcurrencyTest.java
│       └── EnrollmentServiceTest.java
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

**파일 개수 요약:**
- Entity: 4개
- Repository: 2개
- Service: 2개
- Controller: 3개
- DTO: 7개
- Exception: 9개
- Test: 2개
- **총 30개 파일**

---

## 🎯 구현 완료도

### ✅ 필수 구현 (100%)

| 항목 | 상태 | 설명 |
|------|------|------|
| 강의 관리 | ✅ | 등록, 상태 변경, 목록, 상세 조회 |
| 신청 관리 | ✅ | 신청, 확정, 취소, 목록 조회 |
| 정원 관리 | ✅ | 정원 초과 방지, 동시성 제어 |

### ✅ 선택 구현 (75%)

| 항목 | 상태 | 설명 |
|------|------|------|
| 취소 기간 제한 | ✅ | 7일 제한 구현 |
| 수강생 목록 | ✅ | 강의별 수강생 조회 |
| 페이지네이션 | ✅ | 모든 목록에 적용 |
| 대기열 | ❌ | 미구현 (시간 부족) |

**전체 완료도: 90%+**

---

## 📚 추가 자료

- **Postman Collection**: `liveclass-postman.json`
- **ERD 문서**: `ERD_AND_SCHEMA.txt`
- **Docker 가이드**: `DOCKER_DEPLOYMENT_GUIDE.txt`
- **구현 체크리스트**: `IMPLEMENTATION_CHECKLIST.txt`

---

## 📞 문의 및 피드백

프로젝트에 대한 질문이나 피드백은 다음을 통해 주시면 감사하겠습니다:
- **이메일**: mark0302@example.com

---

**마지막 업데이트**: 2026년 5월 19일  
**프로젝트 마감일**: 2026년 5월 23일
