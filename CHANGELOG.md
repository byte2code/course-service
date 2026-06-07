# Changelog

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
