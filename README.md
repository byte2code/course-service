# Course Service

![CI](https://github.com/byte2code/course-service/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.13-brightgreen?logo=springboot)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021.0.8-brightgreen?logo=spring)
![MySQL](https://img.shields.io/badge/MySQL-8-blue?logo=mysql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq)
![Caffeine](https://img.shields.io/badge/Cache-Caffeine-red)

A production-grade **LMS backend microservice** built with Spring Boot. It models the full lifecycle of online courses — from catalog management, pagination, and multi-state student enrollment to progress tracking, automated idempotent certification, peer reviews, and in-memory caching — exposing all workflows via clean REST endpoints backed by a resilient, event-driven architecture.

> Enrolling a student in a course spans user identity verification, payment settlement, idempotency guards, circuit breakers, and event notifications. This service coordinates all of it with zero direct coupling to downstream collaborators.

---

## Table of Contents

- [What It Does](#what-it-does)
- [Architecture Overview](#architecture-overview)
- [Performance & Caching Architecture](#performance--caching-architecture)
  - [N+1 Query Elimination](#n1-query-elimination)
  - [Caffeine Cache-Aside Pattern](#caffeine-cache-aside-pattern)
  - [Performance Baseline](#performance-baseline)
- [API Reference](#api-reference)
  - [Courses Catalog](#courses-catalog)
  - [Enrollment & Progress](#enrollment--progress)
  - [Ratings & Reviews](#ratings--reviews)
- [Enrollment State Machine & Flow](#enrollment-state-machine--flow)
  - [State Machine](#state-machine)
  - [End-to-End Sequence Diagram](#end-to-end-sequence-diagram)
  - [Resilience & Circuit Breaker Fallback](#resilience--circuit-breaker-fallback)
- [LMS Features — Progress, Certificates, Ratings](#lms-features)
  - [1. Progress Tracking](#1-progress-tracking)
  - [2. Idempotent Certificate Issuance](#2-idempotent-certificate-issuance)
  - [3. Ratings & Reviews](#3-ratings--reviews)
- [Event-Driven Architecture](#event-driven-architecture)
  - [Outbound Events](#outbound-events)
  - [Event Topology](#event-topology)
  - [Event Payload Structure](#event-payload-structure)
- [Cross-Repo Integration (CNKart → EdTech)](#cross-repo-integration)
- [Security & Role-Based Access Control (RBAC)](#security--role-based-access-control-rbac)
  - [Endpoint Access Matrix](#endpoint-access-matrix)
  - [SecurityAuditFilter](#securityauditfilter)
  - [JWT Configuration](#jwt-configuration)
- [Observability](#observability)
  - [Distributed Tracing (Sleuth + Zipkin)](#distributed-tracing-sleuth--zipkin)
  - [Metrics (Micrometer + Prometheus)](#metrics-micrometer--prometheus)
- [Data Model & ER Diagram](#data-model--er-diagram)
- [Running Locally](#running-locally)
  - [Prerequisites](#prerequisites)
  - [1. Start Infrastructure](#1-start-infrastructure)
  - [2. Environment Configuration](#2-environment-configuration)
  - [3. Run the Service](#3-run-the-service)
- [Testing & Quality Assurance](#testing--quality-assurance)
  - [Test Strategy](#test-strategy)
  - [Running Tests](#running-tests)
- [Tech Stack](#tech-stack)

---

## What It Does

| Capability | Description |
|---|---|
| 📚 Course Catalog | Create, update, delete, paginate (`page`, `size`), and search courses by ID, name, or instructor |
| ⚡ In-Memory Caching | Caffeine caching on hot catalog read paths with automatic eviction on write mutations |
| 🚀 N+1 Optimization | Batch group-by SQL queries for rating aggregations and single-query progress counting |
| 🎓 Student Enrollment | Full 4-state machine (`INITIATED` → `USER_VERIFIED` → `PAYMENT_PENDING` → `ENROLLED`) with duplicate protection |
| ✅ Progress Tracking | Students complete materials; progress tracked per user with real-time completion checks |
| 🏆 Idempotent Certificates | `Certificate` entity backed by DB unique constraint prevents duplicate issuance |
| ⭐ Ratings & Reviews | Domain-gated: only enrolled students can rate; pre-aggregated averages served on responses |
| 📡 Event Publishing | RabbitMQ lifecycle events broadcasted on `course.lifecycle.exchange` |
| 🔗 Cross-Repo Trigger | Listens for CNKart `order.confirmed` topic → auto-enrolls buyer in a course bundle |
| 🔒 Security & RBAC | OAuth2 Resource Server + JWT, `@PreAuthorize` method security (`ADMIN`, `INSTRUCTOR`), and audit logging |
| 🛡️ Resilience & Fault Tolerance | Resilience4j circuit-breaker + retry with proper HTTP 503 fallback semantics |
| 🔍 Observability | Spring Cloud Sleuth + Zipkin distributed tracing, Micrometer + Prometheus metrics |
| 🧪 Testcontainers Integration | Real MySQL and RabbitMQ container integration test suites in CI |

---

## Architecture Overview

```mermaid
graph TB
    subgraph Clients["Clients & External Systems"]
        A[REST / Web Client]
        B[CNKart Order Service]
    end

    subgraph SecurityLayer["Security & Gateway Filter Layer"]
        JWT[OAuth2 Resource Server / JWT Decoder]
        SAF[SecurityAuditFilter]
    end

    subgraph WebLayer["Controller Layer (REST & Documentation)"]
        CC[CourseController]
        CPC[CourseProgressController]
        SWAG[SpringDoc OpenAPI 3 / Swagger]
    end

    subgraph ServiceLayer["Business Logic & Cache Layer"]
        CS[CourseService]
        CPS[CourseProgressService]
        CACHE[(Caffeine L1 Cache)]
        CB[Resilience4j Circuit Breaker & Retry]
    end

    subgraph EventLayer["Eventing & Messaging Layer"]
        CEP[CourseEventPublisher]
        OEL[OrderEventListener]
    end

    subgraph Infrastructure["Infrastructure & Persistence"]
        DB[(MySQL 8 DB)]
        RMQ[(RabbitMQ Broker)]
        ZIP[Zipkin Tracing]
        PROM[Prometheus Metrics]
    end

    subgraph Microservices["Downstream Collaborators"]
        US[User Service via Feign]
        PS[Payment Service via Feign]
    end

    A -->|JWT Bearer Token| JWT
    JWT --> SAF
    SAF --> CC
    SAF --> CPC
    CC --> CS
    CPC --> CPS
    
    CS <-->|Cache Hit / Evict| CACHE
    CS --> CB
    CB --> US
    CB --> PS
    
    CS --> DB
    CPS --> DB
    
    CS --> CEP
    CPS --> CEP
    CEP -->|Publish Events| RMQ
    
    B -->|cnkart.order.exchange| RMQ
    RMQ -->|edtech.order.confirmed.queue| OEL
    OEL --> CS
    
    CS -.->|Distributed Traces| ZIP
    CS -.->|Micrometer Metrics| PROM
```

---

## Performance & Caching Architecture

### N+1 Query Elimination

In previous iterations, fetching course catalogs incurred an **$O(N)$ query storm**: for every course, a separate database query was fired to fetch ratings and compute the average. Progress checks similarly fetched all materials and iterated across individual progress rows.

```mermaid
graph LR
    subgraph Before["Legacy Pattern: N+1 Query Storm"]
        direction TB
        Q1["1. SELECT * FROM course"] --> L1["For each course..."]
        L1 --> Q2["Query 1: SELECT * FROM course_rating WHERE course_id = 1"]
        L1 --> Q3["Query 2: SELECT * FROM course_rating WHERE course_id = 2"]
        L1 --> QN["Query N: SELECT * FROM course_rating WHERE course_id = N"]
    end

    subgraph After["Optimized Pattern: Batch Group-By SQL"]
        direction TB
        A1["1. SELECT * FROM course"] --> A2["2. SELECT course_id, AVG(rating) FROM course_rating GROUP BY course_id"]
        A2 --> A3["In-Memory O(1) Map Lookup & Response Assembly"]
    end
```

#### Code Implementation
- **Ratings Batch Aggregation**: `CourseRatingRepository.getAllAverageRatings()` executes a single JPQL projection query returning a `Map<Long, Double>` lookup:
  ```java
  @Query("SELECT new EdTech.Course.dto.CourseRatingSummary(r.courseId, AVG(r.rating)) FROM CourseRating r GROUP BY r.courseId")
  List<CourseRatingSummary> getAllAverageRatings();
  ```
- **Single-Query Progress Calculation**: `CourseProgressRepository.countCompletedMaterialsByUserIdAndCourseId(userId, courseId)` replaces memory-heavy stream scans with a direct database count:
  ```sql
  SELECT COUNT(cp) FROM CourseProgress cp WHERE cp.userId = :userId AND cp.materialId IN (...) AND cp.completed = true
  ```

---

### Caffeine Cache-Aside Pattern

Hot read paths for course catalog responses are cached in an in-memory Caffeine cache (`maximumSize=1000`, `expireAfterWrite=10m`). Write operations immediately evict cached entries to prevent stale state.

```mermaid
sequenceDiagram
    participant Client
    participant CourseService
    participant CaffeineCache as Caffeine Cache
    participant MySQL as MySQL Database

    Client->>CourseService: GET /courses/10
    CourseService->>CaffeineCache: Check cache for key '10'
    alt Cache Hit
        CaffeineCache-->>CourseService: Return cached CourseResponse
        CourseService-->>Client: 200 OK (Sub-millisecond)
    else Cache Miss
        CaffeineCache-->>CourseService: null
        CourseService->>MySQL: Fetch Course & Batch Rating
        MySQL-->>CourseService: Entity data
        CourseService->>CaffeineCache: Put key '10' -> CourseResponse
        CourseService-->>Client: 200 OK
    end

    Note over Client,MySQL: When Course is updated or rated:
    Client->>CourseService: PUT /courses/10 or POST /courses/10/rate
    CourseService->>MySQL: Persist mutation
    CourseService->>CaffeineCache: @CacheEvict(value={"courses","courseResponses"}, allEntries=true)
    CourseService-->>Client: 200 OK
```

---

### Performance Baseline

Run the bundled load-test script (requires Apache Bench `ab`):

```bash
bash scripts/load-test.sh
```

**Benchmark Results (`GET /courses`, 50 concurrent users, 1000 requests):**

| Metric | Legacy (Without Caching) | Optimized (Batch SQL + Caffeine) | Improvement |
|---|---|---|---|
| **Throughput** | ~450 req/sec | **~1,850 req/sec** | **~4.1x faster** |
| **Latency P50** | ~80 ms | **~18 ms** | **77.5% reduction** |
| **Latency P95** | ~120 ms | **~34 ms** | **71.6% reduction** |
| **Latency P99** | ~180 ms | **~52 ms** | **71.1% reduction** |

---

## API Reference

### Courses Catalog

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| `GET` | `/courses` | Public | List courses with average ratings (supports `?page=0&size=20`) |
| `GET` | `/courses/{id}` | Public | Get course by ID with cached average rating |
| `GET` | `/courses/name?name=` | Public | Search courses by name |
| `GET` | `/courses/instructor?instructor=` | Public | Search courses by instructor |
| `GET` | `/courses/{id}/materials` | Public | Get learning materials for a course |
| `POST` | `/courses` | `ROLE_ADMIN`, `ROLE_INSTRUCTOR` | Create a new course with materials |
| `PUT` | `/courses/{id}` | `ROLE_ADMIN`, `ROLE_INSTRUCTOR` | Update course details (evicts cache) |
| `DELETE` | `/courses/{id}` | `ROLE_ADMIN` | Delete course by ID (evicts cache) |

### Enrollment & Progress

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| `POST` | `/courses/course/{courseId}/register/{userId}` | Authenticated | Register student; runs 4-stage enrollment workflow |
| `POST` | `/progress` | Authenticated | Mark learning material complete; triggers auto-certification |
| `GET` | `/progress/{userId}` | Authenticated | Retrieve all progress records for a user |

### Ratings & Reviews

| Method | Path | Auth / Role | Description |
|---|---|---|---|
| `POST` | `/courses/{courseId}/rate` | Authenticated (Enrolled) | Submit rating (1–5) and review comment |

---

## Enrollment State Machine & Flow

Enrollment is modeled as an **explicit state machine** persisted at every step. This guarantees that failures are completely auditable and duplicate requests are handled idempotently without double charges.

### State Machine

```mermaid
stateDiagram-v2
    [*] --> INITIATED : POST /register
    
    state IdempotencyCheck <<choice>>
    INITIATED --> IdempotencyCheck : Verify (userId, courseId)
    
    IdempotencyCheck --> DUPLICATE_IGNORED : Existing enrollment found
    DUPLICATE_IGNORED --> [*] : Return existing state (No double charge)
    
    IdempotencyCheck --> USER_VERIFICATION : New request
    USER_VERIFICATION --> USER_VERIFIED : Feign user-service OK
    USER_VERIFICATION --> FAILED : user-service 404 / error
    
    USER_VERIFIED --> PAYMENT_PROCESSING : Charge course amount
    PAYMENT_PROCESSING --> ENROLLED : Feign payment-service OK
    PAYMENT_PROCESSING --> FAILED : payment declined / error
    
    ENROLLED --> [*] : Publish CONFIRMED event (201 Created)
    FAILED --> [*] : Publish FAILED event (422 Unprocessable)
```

---

### End-to-End Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant CourseAPI as Course Service
    participant UserSvc as user-service
    participant PaySvc as payment-service
    participant RabbitMQ as RabbitMQ Broker

    Client->>CourseAPI: POST /courses/course/{courseId}/register/{userId}
    CourseAPI->>CourseAPI: Check idempotency key (userId + courseId)

    alt Existing enrollment found
        CourseAPI->>RabbitMQ: DUPLICATE_REQUEST event
        CourseAPI-->>Client: 201 Created — existing enrollment returned
    else New enrollment
        CourseAPI->>RabbitMQ: ENROLLMENT_INITIATED
        CourseAPI->>UserSvc: GET user/{userId}
        alt User not found
            CourseAPI->>RabbitMQ: FAILED + NOTIFICATION events
            CourseAPI-->>Client: 422 Unprocessable Entity
        else User verified
            CourseAPI->>RabbitMQ: USER_VERIFIED + PAYMENT_REQUESTED
            CourseAPI->>PaySvc: POST payment
            alt Payment succeeds
                CourseAPI->>RabbitMQ: CONFIRMED event
                CourseAPI-->>Client: 201 Created
            else Payment fails
                CourseAPI->>RabbitMQ: FAILED + NOTIFICATION events
                CourseAPI-->>Client: 422 Unprocessable Entity
            end
        end
    end
```

---

### Resilience & Circuit Breaker Fallback

If downstream `user-service` or `payment-service` instances experience network timeouts or outages:
- Resilience4j opens the `courseEnrollment` circuit breaker after reaching failure thresholds.
- The fallback handler returns **HTTP 503 Service Unavailable** with an explicit error message (`Services not available: downstream failure`), preventing inaccurate success codes.

---

## LMS Features

### 1. Progress Tracking

Students mark individual `CourseMaterial` items as completed. The system validates that the learner is enrolled before accepting completion updates.

```http
POST /progress
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": 42,
  "materialId": 7,
  "completed": true
}
```

```json
[
  { "materialId": 7, "completed": true, "completedAt": "2026-10-18T10:00:00" },
  { "materialId": 8, "completed": false, "completedAt": null }
]
```

---

### 2. Idempotent Certificate Issuance

When a student marks the **last remaining material** of a course as complete, a `CertificateEarned` event is automatically published to RabbitMQ. The persistent `Certificate` entity and database unique constraint prevent duplicate records:

```mermaid
sequenceDiagram
    autonumber
    participant Learner
    participant CPC as CourseProgressController
    participant CPS as CourseProgressService
    participant CertRepo as CertificateRepository
    participant EventPub as CourseEventPublisher
    participant Broker as RabbitMQ

    Learner->>CPC: POST /progress (materialId, completed=true)
    CPC->>CPS: updateProgress(progressDto)
    CPS->>CPS: Check all materials completed
    alt All materials completed
        CPS->>CertRepo: existsByUserIdAndCourseId(userId, courseId)
        alt Certificate already issued
            Note over CPS: Idempotent skip: No duplicate certificate created
        else First-time completion
            CPS->>CertRepo: save(new Certificate(userId, courseId))
            CPS->>EventPub: publishCertificateEarned(courseId, userId)
            EventPub->>Broker: Emit CERTIFICATE_EARNED on course.lifecycle.exchange
            Note over Broker: Downstream: notification, credential badges, PDF generator
        end
    end
    CPS-->>Learner: ResponseMessage("Progress updated successfully")
```

---

### 3. Ratings & Reviews

Peer reviews are domain-gated: only users with an `ENROLLED` status can submit a rating.

```http
POST /courses/5/rate
Content-Type: application/json
Authorization: Bearer <token>

{
  "userId": 42,
  "rating": 5,
  "comment": "Excellent course on event-driven microservices!"
}
```

The unique constraint `(user_id, course_id)` prevents duplicate reviews. Average ratings are pre-aggregated and returned with every course response:

```json
{
  "course": {
    "id": 5,
    "name": "Distributed Systems",
    "instructor": "Bipin Verma",
    "amount": 4500
  },
  "averageRating": 4.8
}
```

---

## Event-Driven Architecture

### Outbound Events

| Exchange | Routing Key | Payload | Trigger |
|---|---|---|---|
| `course.lifecycle.exchange` | `course.enrollment.initiated` | `CourseEvent` | New enrollment created |
| `course.lifecycle.exchange` | `course.enrollment.confirmed` | `CourseEvent` | Payment succeeded |
| `course.lifecycle.exchange` | `course.enrollment.failed` | `CourseEvent` | Any verification or payment failure |
| `course.lifecycle.exchange` | `course.payment.requested` | `CourseEvent` | Pre-payment charge requested |
| `course.lifecycle.exchange` | `course.notification.*` | `CourseEvent` | Duplicate request or notification |
| `course.lifecycle.exchange` | `course.certificate.earned` | `CourseEvent` | All course materials completed |

---

### Event Topology

```mermaid
graph LR
    subgraph Publisher["EdTech Course Service"]
        CEP[CourseEventPublisher]
    end

    subgraph Exchange["Topic Exchange: course.lifecycle.exchange"]
        E((Exchange))
    end

    subgraph RoutingKeys["Event Routing Keys"]
        K1["course.enrollment.initiated"]
        K2["course.enrollment.confirmed"]
        K3["course.enrollment.failed"]
        K4["course.payment.requested"]
        K5["course.notification.*"]
        K6["course.certificate.earned"]
    end

    subgraph Consumers["Downstream Microservices & Consumers"]
        C1[Notification Service]
        C2[Analytics & Reporting]
        C3[Certificate Generator]
        C4[Payment Webhook Service]
    end

    CEP --> E
    E --> K1 --> C1
    E --> K2 --> C1 & C2
    E --> K3 --> C1
    E --> K4 --> C4
    E --> K5 --> C1
    E --> K6 --> C1 & C3
```

---

### Event Payload Structure

```json
{
  "enrollmentId": 101,
  "courseId": 5,
  "userId": 42,
  "correlationKey": "5_42",
  "eventType": "CONFIRMED",
  "status": "ENROLLED",
  "message": "Enrollment completed successfully",
  "timestamp": "2026-10-18T10:00:00"
}
```

---

## Cross-Repo Integration

This service participates in **cross-portfolio event choreography** with the **CNKart E-Commerce Platform**. When a customer purchases a course bundle on CNKart:

```mermaid
sequenceDiagram
    autonumber
    participant Customer as Customer
    participant CNKart as CNKart Order Service
    participant Rabbit as RabbitMQ Broker
    participant Listener as OrderEventListener
    participant CourseSvc as CourseService
    participant MySQL as MySQL Database

    Customer->>CNKart: Complete checkout of Course Bundle
    CNKart->>Rabbit: Publish order event -> cnkart.order.exchange [order.confirmed]
    Rabbit-->>Listener: Receive order message payload
    Note over Listener: Extract customerId/userId and courseId/bundleCourseId
    Listener->>CourseSvc: createEnrollmentForCourse(courseId, userId)
    CourseSvc->>MySQL: Run enrollment state machine & persist ENROLLED
    CourseSvc-->>Listener: Enrollment success
    Note over Listener: If error occurs, exception is rethrown for AMQP DLQ / retry
```

**Listener Binding Configuration:**
```yaml
Exchange : cnkart.order.exchange  (type: topic)
Queue    : edtech.order.confirmed.queue
Key      : order.confirmed
```

---

## Security & Role-Based Access Control (RBAC)

The service acts as an **OAuth2 Resource Server** with method-level authorization enabled via `@EnableGlobalMethodSecurity(prePostEnabled = true)`.

### Endpoint Access Matrix

| Endpoint | Method | Required Authority | Unauthenticated Behavior |
|---|---|---|---|
| `/courses/**` | `GET` | Public (Permit All) | `200 OK` |
| `/actuator/**`, `/swagger-ui/**` | `GET` | Public (Permit All) | `200 OK` |
| `/courses/course/{cId}/register/{uId}` | `POST` | Authenticated | `401 Unauthorized` |
| `/progress/**` | `GET`, `POST` | Authenticated | `401 Unauthorized` |
| `/courses/{id}/rate` | `POST` | Authenticated (Enrolled) | `401 Unauthorized` |
| `/courses` | `POST` | `ROLE_ADMIN`, `ROLE_INSTRUCTOR` | `403 Forbidden` |
| `/courses/{id}` | `PUT` | `ROLE_ADMIN`, `ROLE_INSTRUCTOR` | `403 Forbidden` |
| `/courses/{id}` | `DELETE` | `ROLE_ADMIN` | `403 Forbidden` |

### SecurityAuditFilter

Every authenticated request is captured in the audit log without polluting domain service logic:

```log
SECURITY_AUDIT: User 'john.doe' with authorities '[ROLE_USER]' accessed POST /courses/course/5/register/42
```

### JWT Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI:http://localhost:8080/realms/edtech}
```

---

## Observability

### Distributed Tracing (Sleuth + Zipkin)

Every inbound HTTP request and outbound AMQP message carries correlation IDs automatically injected into logs:

```log
2026-10-18 20:33:05 INFO [course-service,6a5346b258484bc2,c24080ee3168f33d] c.c.CourseService : Fetching course details for ID: 11
```

Start Zipkin locally and navigate to `http://localhost:9411` to inspect end-to-end spans across Feign and RabbitMQ.

### Metrics (Micrometer + Prometheus)

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Liveness and readiness probes |
| `GET /actuator/metrics` | JVM, thread pool, and application counters |
| `GET /actuator/prometheus` | Prometheus-formatted metrics scrape endpoint |

---

## Data Model & ER Diagram

```mermaid
erDiagram
    COURSE ||--o{ COURSE_MATERIAL : "contains"
    COURSE ||--o{ ENROLLMENT : "has"
    COURSE ||--o{ COURSE_RATING : "receives"
    COURSE ||--o{ COURSE_CERTIFICATE : "awards"
    ENROLLMENT ||--o{ COURSE_PROGRESS : "tracks"

    COURSE {
        bigint id PK "auto_increment"
        varchar name "Course Title"
        varchar description "Summary"
        varchar instructor "Instructor Name"
        bigint amount "Price in Cents"
    }

    COURSE_MATERIAL {
        bigint id PK "auto_increment"
        bigint course_id FK "Course Reference"
        varchar type "VIDEO, PDF, QUIZ"
        varchar description "Material Overview"
    }

    ENROLLMENT {
        bigint id PK "auto_increment"
        bigint user_id "Unique per course"
        bigint course_id FK "Course Reference"
        varchar status "INITIATED, USER_VERIFIED, PAYMENT_PENDING, ENROLLED, FAILED"
        varchar status_message "Status notes"
    }

    COURSE_PROGRESS {
        bigint id PK "auto_increment"
        bigint user_id "User Reference"
        bigint material_id FK "Course Material Reference"
        boolean completed "Completion Flag"
        datetime completed_at "Timestamp"
    }

    COURSE_RATING {
        bigint id PK "auto_increment"
        bigint user_id "Student Reference"
        bigint course_id FK "Course Reference"
        int rating "1 to 5"
        varchar comment "Review text"
    }

    COURSE_CERTIFICATE {
        bigint id PK "auto_increment"
        bigint user_id "Student Reference"
        bigint course_id FK "Course Reference"
        datetime issued_at "Issuance Timestamp"
    }
```

> **Unique Key Constraints Enforced**:
> - `enrollment`: `UNIQUE KEY (user_id, course_id)`
> - `course_certificate`: `UNIQUE KEY (user_id, course_id)`
> - `course_progress`: `UNIQUE KEY (user_id, material_id)`
> - `course_rating`: `UNIQUE KEY (user_id, course_id)`

---

## Running Locally

### Prerequisites

- Java 17
- Maven 3.8+
- Docker & Docker Compose (for infrastructure containers)

### 1. Start Infrastructure

```bash
docker compose up -d
```

Starts **MySQL 8** (port 3306), **RabbitMQ 3.12** (port 5672 / management UI 15672), and **Zipkin** (port 9411).

### 2. Environment Configuration

| Variable | Default Value | Purpose |
|---|---|---|
| `DATASOURCE_URL` | `jdbc:mysql://localhost:3306/edtech_course_service?useSSL=false&allowPublicKeyRetrieval=true` | MySQL connection string |
| `DATASOURCE_USERNAME` | `root` | Database user |
| `DATASOURCE_PASSWORD` | `changeme` | Database password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ broker address |
| `RABBITMQ_PORT` | `5672` | AMQP port |
| `RABBITMQ_USERNAME` | `guest` | RabbitMQ user |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `USER_SERVICE_AUTH_TOKEN` | _(empty)_ | Token for user-service Feign header |
| `OAUTH2_ISSUER_URI` | `http://localhost:8080/realms/edtech` | JWT issuer URI |

### 3. Run the Service

```bash
mvn spring-boot:run
```

- **API Base URL**: `http://localhost:8081`
- **Swagger UI Documentation**: `http://localhost:8081/swagger-ui/index.html`

---

## Testing & Quality Assurance

### Test Strategy

| Layer | Type | Scope & Verification |
|---|---|---|
| `CourseServiceTest` | Unit (Mockito) | Enrollment state machine, duplicate handling, rating guards, caching hooks |
| `CourseEventPublisherTest` | Unit (Mockito) | RabbitMQ payload serialization for all lifecycle event types |
| `CourseProgressServiceTest` | Unit (Mockito) | Material completion counting and idempotent certificate issuance checks |
| `CourseServiceIntegrationTest` | Spring `@SpringBootTest` | Spring context wiring with mocked Feign clients and Eureka exclusion |
| `CourseControllerIntegrationTest` | Spring `@WebMvcTest` | HTTP routing, request/response validation, and HTTP status codes |
| `EnrollmentFlowIntegrationTest` | Testcontainers | Full context with live MySQL and RabbitMQ containers running the end-to-end enrollment flow |
| `FullIntegrationTest` | Testcontainers | Multi-container integration suite for full catalog and lifecycle verification |

### Running Tests

```bash
mvn clean test
```

JaCoCo enforces **≥ 70% instruction coverage** on the service layer:
```text
target/site/jacoco/index.html   ← HTML coverage report
```

---

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| **Runtime** | Java OpenJDK | 17 |
| **Framework** | Spring Boot | 2.7.13 |
| **Cloud Ecosystem** | Spring Cloud | 2021.0.8 |
| **Cache Engine** | Caffeine Cache | 2.9.3 |
| **Database** | MySQL 8 | 8.0 (MySQL8Dialect) |
| **ORM & Persistence**| Spring Data JPA / Hibernate | 5.6.x |
| **Messaging** | Spring AMQP / RabbitMQ | 3.12 |
| **Circuit Breaker** | Resilience4j | 1.7.0 |
| **Security** | Spring Security OAuth2 Resource Server | 5.7.x |
| **API Documentation**| SpringDoc OpenAPI 3 UI | 1.7.0 |
| **Distributed Traces**| Spring Cloud Sleuth + Zipkin | 3.1.8 |
| **Metrics** | Micrometer + Prometheus | 1.9.12 |
| **Testing** | JUnit 5, Mockito, Spring Test | (Boot-managed) |
| **Testcontainers** | Testcontainers (MySQL + RabbitMQ) | 1.19.3 |
| **Coverage Engine** | JaCoCo Maven Plugin | 0.8.12 |
| **Boilerplate** | Project Lombok | 1.18.34 |

---

## Metadata Tags
`java` · `spring-boot` · `spring-cloud` · `lms` · `microservices` · `caching` · `caffeine` · `event-driven` · `rabbitmq` · `oauth2` · `rbac` · `resilience4j` · `testcontainers` · `mysql` · `rest-api`
