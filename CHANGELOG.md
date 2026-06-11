# Changelog

## v9.2.0 - 2026-06-11

- Added global exception handler to map `RuntimeException` to 422 Unprocessable Entity for enrollment errors

## v9.1.0 - 2026-06-11

- Renamed `courseController` class to `CourseController` to follow standard Java PascalCase naming conventions

## v9.0.0 - 2026-06-07

- Added Spring-based integration-style tests for the controller and service layers
- Expanded the README with a clearer enrollment sequence flow, failure branches, and messaging details
- Added metadata tags to improve discoverability for backend and recruiter searches
- Removed the stray `HELP.md` file from the repository root

## v8.0.0 - 2026-06-07

- Added RabbitMQ-backed event publishing for enrollment, payment, and notification workflows
- Emitted lifecycle events for enrollment initiated, user verified, payment requested, confirmed, failed, and duplicate-request paths
- Kept the synchronous enrollment contract unchanged while making downstream integrations easier to add
- Updated the README with the event flow, messaging details, and broker configuration notes

## v7.0.0 - 2026-06-05

- Added idempotent enrollment handling for repeated user/course registration requests
- Reused the existing enrollment record instead of creating duplicate enrollments or duplicate payment calls
- Added a unique constraint on the enrollment user/course pair
- Updated the README to explain the idempotent enrollment path

## v6.0.0 - 2026-06-03

- Added an explicit enrollment lifecycle with `INITIATED`, `USER_VERIFIED`, `PAYMENT_PENDING`, `ENROLLED`, and `FAILED`
- Persisted enrollment state changes step-by-step so payment and user-verification failures remain visible in the database
- Updated the README with the new enrollment state machine and flow diagram

## v5.0.0 - 2026-05-10

- Added Feign clients for user and payment services
- Moved enrollment coordination to Feign-based service calls
- Kept the Hystrix-backed fallback path in place

## v4.0.0 - 2026-05-10

- Added Hystrix fallback handling for course enrollment
- Added circuit-breaker related configuration
- Kept the service-to-service enrollment and payment flow in place

## v3.0.0 - 2026-05-10

- Added load-balanced RestTemplate configuration
- Added payment forwarding during enrollment
- Switched enrollment flow to coordinated user and payment service calls

## v2.0.0 - 2026-05-10

- Added course enrollment support
- Added user verification before enrollment
- Added `POST /courses/course/{courseId}/register/{userId}`
- Expanded the service layer to prepare for future payment integration

## v1.0.0 - 2026-05-10

- Initial publication of the Course service
- Added the first full course, material, and enrollment API surface
