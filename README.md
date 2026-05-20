# 🎓 수강 신청 시스템 (LiveClass Enrollment System)

라이브클래스 채용 과제 - Backend 과제 (BE-A)

---

## 📋 프로젝트 개요

온라인 강의 플랫폼의 **수강 신청 시스템**을 구현한 Spring Boot REST API 서버입니다.

### 🎯 핵심 기능

- **사용자 관리**: 강사(Creator) / 학생(Student) 회원가입 (Role 기반)
- **강의 관리**: 강의 등록, 상태 관리 (DRAFT → OPEN → CLOSED)
- **수강 신청**: 신청, 결제 확정, 취소 (PENDING → CONFIRMED → CANCELLED)
- **정원 관리**: 정원 초과 방지, 동시성 제어 (비관적 락)
- **페이지네이션**: 모든 목록 조회에 적용
- **취소 기간 제한**: 확정 후 7일 이내만 취소 가능

### ✨ 특징

- ✅ **Role 기반 권한**: User entity의 role (CREATOR/STUDENT/ADMIN) 기반 검증
- ✅ **동시성 제어**: 비관적 락(PESSIMISTIC_WRITE)으로 Race Condition 방지
- ✅ **데이터 무결성**: JPA Cascade, UNIQUE 제약으로 보장
- ✅ **에러 처리**: 9개의 커스텀 Exception + GlobalExceptionHandler
- ✅ **Swagger UI**: API 문서 자동 생성
- ✅ **테스트**: 50명 동시 신청 동시성 테스트 포함
- ✅ **Railway 배포**: 클라우드 배포 가능

---

## 🛠️ 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.4.2 |
| **Build Tool** | Maven | 3.9+ |
| **Database** | H2 In-Memory | 2.3.232 |
| **ORM** | Spring Data JPA | 3.4.2 |
| **Security** | Spring Security | 6.4.2 |
| **API Docs** | Springdoc OpenAPI | 2.5.0 |
| **Testing** | JUnit 5, AssertJ | Latest |
| **Deployment** | Railway | - |

---

## 🚀 실행 방법

### 1️⃣ 로컬 실행 (Maven)

```powershell
# 프로젝트 디렉토리로 이동
cd C:\Users\pc\IdeaProjects\liveclass

# Maven 빌드 및 실행
mvn clean spring-boot:run
```

**확인:**
```
http://localhost:8080/api/swagger-ui.html
http://localhost:8080/api/h2-console
```

### 2️⃣ IDE 실행 (IntelliJ)

1. **Run** → **Run 'EnrollmentSystemApplication'**
2. 또는 **Shift + F10**

### 3️⃣ Railway 클라우드 배포 (권장 🌐)

상세 가이드: [Railway 배포 섹션](#-railway-배포-가이드) 참조

---

## 📊 데이터 모델 (ERD)

### 테이블 관계도

```
┌─────────────────────┐
│      APP_USER       │
├─────────────────────┤
│ PK: id (VARCHAR)    │
│ name                │
│ email (UNIQUE)      │
│ role (ENUM)         │ ← STUDENT/CREATOR/ADMIN
│ created_at          │
│ updated_at          │
└─────────────────────┘
         ▲
         │ 1:1
         │ (id 공유)
    ┌────┴─────┐
    │          │
┌───┴────┐  ┌─┴──────┐
│CREATOR │  │STUDENT │
├────────┤  ├────────┤
│PK: id  │  │PK: id  │
│bio     │  │bio     │
│expert. │  │phone   │
│total_  │  │enrolled│
│students│  │_at     │
│avg_    │  │updated │
│rating  │  │_at     │
└────────┘  └────────┘
     │
     │ 1:N
     ▼
┌─────────────────────┐
│      COURSE         │
├─────────────────────┤
│ PK: id (UUID)       │
│ FK: creator_id      │ → CREATOR.id
│ title               │
│ description         │
│ price               │
│ max_capacity        │
│ current_enrollment  │
│ status (ENUM)       │ ← DRAFT/OPEN/CLOSED
│ start_date          │
│ end_date            │
│ created_at          │
│ updated_at          │
└─────────────────────┘
         ▲
         │ 1:N
         │ (course_id)
         │
┌────────┴────────────┐
│    ENROLLMENT       │
├─────────────────────┤
│ PK: id (UUID)       │
│ course_id           │ → COURSE.id
│ user_id             │ → APP_USER.id
│ status (ENUM)       │ ← PENDING/CONFIRMED/CANCELLED
│ enrolled_at         │
│ confirmed_at        │
│ cancelled_at        │
│ created_at          │
│ updated_at          │
└─────────────────────┘
```

### APP_USER 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(255) | PK | 사용자 ID (creator-1, student-1) |
| **name** | VARCHAR(255) | NOT NULL | 이름 |
| **email** | VARCHAR(255) | UNIQUE | 이메일 |
| **role** | ENUM | NOT NULL | STUDENT/CREATOR/ADMIN |
| **created_at** | TIMESTAMP | NOT NULL | 생성 시간 |
| **updated_at** | TIMESTAMP | NULL | 수정 시간 |

> ⚠️ **참고**: `USER`는 H2 예약어이므로 테이블명을 `APP_USER`로 변경

### CREATOR 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(255) | PK | APP_USER.id와 동일 |
| **bio** | TEXT | NULL | 강사 소개 |
| **expertise** | VARCHAR(255) | NULL | 전문 분야 |
| **total_students** | INT | DEFAULT 0 | 누적 학생 수 |
| **avg_rating** | DOUBLE | DEFAULT 0.0 | 평균 평점 |
| **enrolled_at** | TIMESTAMP | NULL | 가입일 |
| **updated_at** | TIMESTAMP | NULL | 수정일 |

### STUDENT 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(255) | PK | APP_USER.id와 동일 |
| **bio** | TEXT | NULL | 학생 소개 |
| **phone** | VARCHAR(255) | NULL | 전화번호 (010-XXXX-XXXX) |
| **enrolled_at** | TIMESTAMP | NULL | 가입일 |
| **updated_at** | TIMESTAMP | NULL | 수정일 |

### COURSE 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(255) | PK | UUID 형식 |
| **creator_id** | VARCHAR(255) | FK NOT NULL | CREATOR.id 참조 |
| **title** | VARCHAR(255) | NOT NULL | 강의 제목 |
| **description** | TEXT | NULL | 강의 설명 |
| **price** | INT | NOT NULL | 가격 (원) |
| **max_capacity** | INT | NOT NULL | 최대 정원 |
| **current_enrollment** | INT | DEFAULT 0 | 현재 신청 인원 |
| **status** | ENUM | NOT NULL | DRAFT/OPEN/CLOSED |
| **start_date** | TIMESTAMP | NOT NULL | 강의 시작일 |
| **end_date** | TIMESTAMP | NOT NULL | 강의 종료일 |
| **created_at** | TIMESTAMP | NOT NULL | 생성 시간 |
| **updated_at** | TIMESTAMP | NULL | 수정 시간 |

### ENROLLMENT 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| **id** | VARCHAR(255) | PK | UUID 형식 |
| **course_id** | VARCHAR(255) | NOT NULL | 강의 ID |
| **user_id** | VARCHAR(255) | NOT NULL | 학생 ID |
| **status** | ENUM | NOT NULL | PENDING/CONFIRMED/CANCELLED |
| **enrolled_at** | TIMESTAMP | NOT NULL | 신청 시간 |
| **confirmed_at** | TIMESTAMP | NULL | 결제 확정 시간 |
| **cancelled_at** | TIMESTAMP | NULL | 취소 시간 |
| **created_at** | TIMESTAMP | NOT NULL | 생성 시간 |
| **updated_at** | TIMESTAMP | NULL | 수정 시간 |

---

## 📡 API 목록

### 🔑 인증 방식

모든 인증이 필요한 API는 **Authorization 헤더**에 사용자 ID 전달:
```
Authorization: creator-1   (강사)
Authorization: student-1   (학생)
```

### 👤 Users API

#### 1. 강사 회원가입
```http
POST /api/users/creators
Content-Type: application/json

{
  "id": "creator-1",
  "name": "김강사",
  "email": "creator1@test.com",
  "bio": "Spring 전문가",
  "expertise": "Spring Boot"
}
```

**응답 (201):**
```json
{
  "creatorId": "creator-1",
  "name": "김강사",
  "email": "creator1@test.com",
  "bio": "Spring 전문가",
  "expertise": "Spring Boot",
  "enrolledAt": "2026-05-19T10:30:00",
  "message": "강사 회원가입 성공"
}
```

#### 2. 학생 회원가입
```http
POST /api/users/students
Content-Type: application/json

{
  "id": "student-1",
  "name": "이학생",
  "email": "student1@test.com",
  "bio": "열심히 배우겠습니다",
  "phone": "010-1234-5678"
}
```

**응답 (201):**
```json
{
  "studentId": "student-1",
  "name": "이학생",
  "email": "student1@test.com",
  "bio": "열심히 배우겠습니다",
  "phone": "010-1234-5678",
  "enrolledAt": "2026-05-19T10:30:00",
  "message": "학생 회원가입 성공"
}
```

### 🎬 강의관리 API

#### 3. 강의 생성 (Creator만)
```http
POST /api/courses
Authorization: creator-1
Content-Type: application/json

{
  "title": "Spring Boot 실전",
  "description": "REST API 개발",
  "price": 50000,
  "maxCapacity": 30,
  "startDate": "2026-06-01T10:00:00",
  "endDate": "2026-07-01T10:00:00"
}
```

**응답 (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "creatorId": "creator-1",
  "title": "Spring Boot 실전",
  "description": "REST API 개발",
  "price": 50000,
  "maxCapacity": 30,
  "currentEnrollment": 0,
  "status": "DRAFT",
  "startDate": "2026-06-01T10:00:00",
  "endDate": "2026-07-01T10:00:00",
  "createdAt": "2026-05-19T10:30:00"
}
```

#### 4. 강의 상태 변경 (Creator만)
```http
PATCH /api/courses/{courseId}/status
Authorization: creator-1
Content-Type: application/json

{
  "status": "OPEN"
}
```

#### 5. 강의 목록 조회 (누구나)
```http
GET /api/courses?status=OPEN&page=0&size=20
```

**응답 (200):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Spring Boot 실전",
      "status": "OPEN",
      "maxCapacity": 30,
      "currentEnrollment": 5,
      "price": 50000
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "first": true,
  "last": true
}
```

#### 6. 강의 상세 조회 (누구나)
```http
GET /api/courses/{courseId}
```

#### 7. 내 강의 목록 (Creator만)
```http
GET /api/courses/creator/my-courses?page=0&size=20
Authorization: creator-1
```

### 📝 수강신청 API

#### 8. 수강 신청 (Student만)
```http
POST /api/enrollments?courseId={courseId}
Authorization: student-1
```

**응답 (201):**
```json
{
  "id": "enroll-uuid",
  "userId": "student-1",
  "courseId": "course-uuid",
  "status": "PENDING",
  "enrolledAt": "2026-05-19T10:30:00"
}
```

#### 9. 결제 확정 (Student만)
```http
PATCH /api/enrollments/{enrollmentId}/confirm
Authorization: student-1
```

#### 10. 신청 취소 (Student만)
```http
PATCH /api/enrollments/{enrollmentId}/cancel
Authorization: student-1
```

#### 11. 신청 상세 조회
```http
GET /api/enrollments/{enrollmentId}
Authorization: student-1
```

#### 12. 내 신청 목록 (Student만)
```http
GET /api/enrollments/me?page=0&size=20
Authorization: student-1
```

#### 13. 강의별 수강생 목록 (Creator용)
```http
GET /api/enrollments/course/{courseId}?page=0&size=20
```

---

## 🧪 테스트 실행 방법

### 1️⃣ 전체 테스트 실행

```powershell
mvn test
```

### 2️⃣ 동시성 테스트 실행

```powershell
# 동시성 테스트만 실행
mvn test -Dtest=EnrollmentConcurrencyTest

# 특정 메서드만 실행
mvn test -Dtest=EnrollmentConcurrencyTest#동시_결제확정_정원초과_방지
```

### 3️⃣ 동시성 테스트 시나리오

#### 테스트 1: 동시 수강신청
```
시나리오: 50명이 동시에 신청
검증:     성공 건수 ≤ 50명
```

#### 테스트 2: 동시 결제 확정 (가장 중요)
```
시나리오: 50명이 PENDING → 동시 CONFIRMED 시도
검증:     CONFIRMED 건수 ≤ 정원(10명)
결과:     비관적 락으로 정확히 10명만 성공
```

#### 테스트 3: 중복 신청 방지
```
시나리오: 같은 학생이 10번 동시 신청
검증:     성공 건수 = 1
```

### 4️⃣ 테스트 결과 예시

```
=== 결제 확정 동시성 테스트 결과 ===
✅ 확정 성공: 10
❌ 정원 초과: 40
📊 DB 확정 건수: 10
🎯 정원 제한: 10
==================================

[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

### 5️⃣ Swagger UI로 수동 테스트

1. http://localhost:8080/api/swagger-ui.html 접속
2. 우측 상단 🔒 **Authorize** 버튼 클릭
3. Value에 사용자 ID 입력 (예: `creator-1`)
4. **Authorize** → **Close**
5. API 호출 (Try it out → Execute)

### 6️⃣ 테스트 순서 (수동)

```
1. POST /api/users/creators        → 강사 등록
2. POST /api/users/students        → 학생 등록
3. POST /api/courses               → 강의 생성 (Authorization: creator-1)
4. PATCH /api/courses/{id}/status  → status: OPEN
5. POST /api/enrollments?courseId  → 수강 신청 (Authorization: student-1)
6. PATCH /api/enrollments/{id}/confirm  → 결제 확정
7. PATCH /api/enrollments/{id}/cancel   → 취소 (7일 이내)
```

---

## 📦 프로젝트 구조

```
liveclass/
├── src/
│   ├── main/
│   │   ├── java/com/example/liveclass/
│   │   │   ├── config/                       # 설정
│   │   │   │   ├── SecurityConfig.java       # Spring Security
│   │   │   │   └── SwaggerConfig.java        # OpenAPI/Swagger
│   │   │   │
│   │   │   ├── controller/                   # REST 컨트롤러
│   │   │   │   ├── UserController.java       # 회원가입 API
│   │   │   │   ├── CourseController.java     # 강의 API
│   │   │   │   ├── EnrollmentController.java # 수강신청 API
│   │   │   │   └── GlobalException.java
│   │   │   │
│   │   │   ├── service/                      # 비즈니스 로직
│   │   │   │   ├── CreatorService.java
│   │   │   │   ├── StudentService.java
│   │   │   │   ├── CourseService.java
│   │   │   │   └── EnrollmentService.java
│   │   │   │
│   │   │   ├── repository/                   # JPA 리포지토리
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── CreatorRepository.java
│   │   │   │   ├── StudentRepository.java
│   │   │   │   ├── CourseRepository.java
│   │   │   │   └── EnrollmentRepository.java
│   │   │   │
│   │   │   ├── entity/                       # JPA 엔티티
│   │   │   │   ├── User.java                 # @Table(name="app_user")
│   │   │   │   ├── Creator.java
│   │   │   │   ├── Student.java
│   │   │   │   ├── Course.java + CourseStatus
│   │   │   │   └── Enrollment.java + EnrollmentStatus
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── CreateCreatorRequest.java
│   │   │   │   │   ├── CreateStudentRequest.java
│   │   │   │   │   ├── CreateCourseRequest.java
│   │   │   │   │   ├── UpdateCourseStatusRequest.java
│   │   │   │   │   └── EnrollRequest.java
│   │   │   │   │
│   │   │   │   └── response/
│   │   │   │       ├── CreatorRegistrationResponse.java
│   │   │   │       ├── StudentRegistrationResponse.java
│   │   │   │       ├── CourseResponse.java
│   │   │   │       ├── EnrollmentResponse.java
│   │   │   │       └── PaginatedResponse.java
│   │   │   │
│   │   │   ├── exception/                    # 커스텀 예외
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   ├── DuplicateException.java
│   │   │   │   ├── CourseNotFoundException.java
│   │   │   │   ├── CourseNotOpenException.java
│   │   │   │   ├── EnrollmentNotFoundException.java
│   │   │   │   ├── CapacityExceededException.java
│   │   │   │   ├── DuplicateEnrollmentException.java
│   │   │   │   ├── InvalidStateException.java
│   │   │   │   └── CancellationPeriodExceededException.java
│   │   │   │
│   │   │   └── EnrollmentSystemApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # 메인 설정
│   │       └── application-prod.yml          # Railway 배포용
│   │
│   └── test/java/com/example/liveclass/
│       └── EnrollmentConcurrencyTest.java    # 동시성 테스트
│
├── Dockerfile                                # Docker 이미지
├── railway.json                              # Railway 배포 설정
├── .dockerignore
├── .gitignore
├── pom.xml                                   # Maven 설정
└── README.md
```

**파일 개수:**
- Entity: 7개 (User, Creator, Student, Course, Enrollment + 2 Status)
- Repository: 5개
- Service: 4개
- Controller: 4개
- DTO: 10개
- Exception: 9개
- Config: 2개
- Test: 1개
- **총 42개 파일**

---

## 🎯 설계 결정

### 1. Role 기반 권한 분리

```java
public enum UserRole {
    STUDENT, CREATOR, ADMIN
}
```

각 Service에서 권한 검증:
- **Course 생성/수정**: CREATOR만 가능
- **Enrollment 신청/취소**: STUDENT만 가능

### 2. 비관적 락 (Pessimistic Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT e FROM Enrollment e WHERE e.id = :id")
Optional<Enrollment> findByIdWithLock(@Param("id") String id);
```

**이유**: 정원 초과를 DB 레벨에서 완벽하게 방지

### 3. 테이블명 변경

`USER` → `app_user` (H2 예약어 회피)

### 4. UUID ID 생성

```java
@PrePersist
protected void onCreate() {
    if (this.id == null) {
        this.id = UUID.randomUUID().toString();
    }
}
```

### 5. 상태 단방향 전이

```
Course:    DRAFT → OPEN → CLOSED
Enrollment: PENDING → CONFIRMED → CANCELLED
```

---

## ❌ 미구현 항목

### 미구현 기능

| 기능 | 이유 |
|------|------|
| **대기열(Waitlist)** | 시간 제약, 핵심 기능 우선 |
| **JWT 인증** | Authorization 헤더 방식으로 단순화 |
| **결제 시스템 연동** | 과제 명세상 상태 변경으로 대체 |
| **로깅 시스템** | SLF4J/Logback만 사용 |

### 향후 개선 가능

- [ ] 대기열 자동 승격 시스템
- [ ] JWT 기반 인증
- [ ] PostgreSQL 영구 저장
- [ ] Redis 캐싱
- [ ] 강의 검색/필터링
- [ ] 강의 평점/리뷰
- [ ] 메시지 큐 (RabbitMQ)
- [ ] Prometheus/Grafana 모니터링

---

## 🤖 AI 활용 범위

### 사용한 AI: Claude (Anthropic)

| 항목 | AI 비율 | 개발자 |
|------|---------|--------|
| Entity 설계 | 20% | 80% |
| Repository | 30% | 70% |
| Service 로직 | 40% | 60% |
| Controller | 50% | 50% |
| Exception | 80% | 20% |
| Config (Swagger, Security) | 90% | 10% |
| Test 코드 | 50% | 50% |
| 문서화 | 90% | 10% |
| **평균** | **56%** | **44%** |

### AI 미사용 부분

- 핵심 비즈니스 로직 결정
- 데이터 모델 관계 설계
- 에러 케이스 정의
- 동시성 제어 전략 선택

---

## 📞 문의

- **이메일**: mark0302@example.com

---

**프로젝트 완료일**: 2026년 5월 19일  
**과제 마감일**: 2026년 5월 23일  
**버전**: 1.0.0
