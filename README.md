# Course Service

Spring Boot service for managing courses in an EdTech platform. The application models courses, course materials, and enrollments and exposes REST endpoints for course lookup, lifecycle management, and user enrollment.

## Overview

The service acts as a course catalog and enrollment backend for an EdTech system. Administrators can create and update courses, learners can view course details, and the platform can enroll users into courses while coordinating with user and payment services.

## What it does

- List all courses
- Retrieve courses by id, name, or instructor
- Inspect course materials for a course
- Create, update, and delete courses
- Enroll a user in a course
- Verify users through a user-service Feign client before enrollment
- Create a payment record through a payment-service Feign client after enrollment
- Provide a Hystrix fallback for enrollment failures
- Persist course, material, and enrollment data with JPA
- Return simple success/error messages for course operations

## Main API

- `GET /courses`
- `GET /courses/{id}`
- `GET /courses/name/?name=...`
- `GET /courses/courseMaterial/?id=...`
- `GET /courses/instructor/?instructor=...`
- `POST /courses`
- `PUT /courses/{id}`
- `DELETE /courses/{id}`
- `POST /courses/course/{courseId}/register/{userId}`

## Enrollment Flow

The v5 snapshot moves the service-to-service communication into Feign clients.

1. The API receives a course id and user id.
2. The service calls the user-service client to verify the user exists.
3. If the course exists, an `Enrollment` record is created and stored.
4. The service calls the payment-service client to create a payment record.
5. If the remote call fails, the controller returns a fallback response.

## Integration Points

- `user-service` via `EdTech.Course.feign.UserService`
- `payment-service` via `EdTech.Course.feign.PaymentService`
- `RestTemplate` bean remains available for compatibility with the existing configuration

## Data Model

- `Course` represents the core catalog item
- `CourseMaterial` stores content tied to a course
- `Enrollment` links users to courses
- `CourseDto` is used for create and update requests
- `ResponseMessage` standardizes simple API responses
- `Payment` is used when forwarding enrollment charges to the payment service

## Configuration

- Main application port: `8081`
- Datasource: `edtech_course_service`
- Hibernate is configured for `update` mode
- The service uses Spring Boot 2.7.13
- The app registers a load-balanced `RestTemplate` bean for service-to-service calls
- Hystrix fallback support is enabled for enrollment requests

## Stack

- Java 17
- Spring Boot 2.7.13
- Spring Data JPA
- Spring JDBC
- Spring Web
- Spring Cloud LoadBalancer
- Spring Cloud OpenFeign
- Hystrix
- MySQL
- Lombok

## Notes

- The repository is intended to be extended with additional enrollment and payment workflows.
- The current service layer includes Feign-based user and payment integration.
- Generated build output is intentionally not tracked in git.
