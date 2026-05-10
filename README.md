# Course Service

Spring Boot service for managing courses in an EdTech platform. The application models courses, course materials, and enrollments and exposes REST endpoints for course lookup, lifecycle management, and user enrollment.

## Overview

The service acts as a course catalog and enrollment backend for an EdTech system. Administrators can create and update courses, learners can view course details, and the platform can enroll users into courses after verifying they exist in the user service.

## What it does

- List all courses
- Retrieve courses by id, name, or instructor
- Inspect course materials for a course
- Create, update, and delete courses
- Enroll a user in a course
- Verify users through an external user-service call before enrollment
- Persist course, material, and enrollment data with JPA
- Return simple success/error messages for course operations

## Main API

- `GET /courses`
- `GET /courses/{id}`
- `GET /courses/name?name=...`
- `GET /courses/courseMaterial?id=...`
- `GET /courses/instructor?instructor=...`
- `POST /courses`
- `PUT /courses/{id}`
- `DELETE /courses/{id}`
- `POST /courses/course/{courseId}/register/{userId}`

## Enrollment Flow

The v2 snapshot adds enrollment handling inside `CourseService`.

1. The API receives a course id and user id.
2. The service verifies that the user exists through an external user-service endpoint.
3. If the course exists, an `Enrollment` record is created and stored.
4. The service returns a user-friendly response message.

The current implementation is prepared for future payment integration as well.

## Data Model

- `Course` represents the core catalog item
- `CourseMaterial` stores content tied to a course
- `Enrollment` links users to courses
- `CourseDto` is used for create and update requests
- `ResponseMessage` standardizes simple API responses
- `Payment` is included as a DTO for future payment-oriented workflows

## Configuration

- Main application port: `8081`
- Default datasource placeholders live in `application.yml`
- Hibernate is configured for `update` mode
- The service uses Spring Boot 2.7.13
- MySQL is the default persistence target

## Stack

- Java 17
- Spring Boot 2.7.13
- Spring Data JPA
- Spring JDBC
- Spring Web
- Spring Boot DevTools
- MySQL
- Lombok

## Notes

- The repository is intended to be extended with additional enrollment and payment workflows.
- The current service layer includes external-service calls for user validation.
- Generated build output is intentionally not tracked in git.
