# Course Service

Spring Boot service for managing courses in an EdTech platform. The application models courses, course materials, and enrollments and exposes REST endpoints for course lookup and lifecycle management.

## What it does

- List all courses
- Retrieve courses by id, name, or instructor
- Inspect course materials for a course
- Create, update, and delete courses
- Persist course, material, and enrollment data with JPA

## Main API

- `GET /courses`
- `GET /courses/{id}`
- `GET /courses/name?name=...`
- `GET /courses/courseMaterial?id=...`
- `GET /courses/instructor?instructor=...`
- `POST /courses`
- `PUT /courses/{id}`
- `DELETE /courses/{id}`

## Data Model

- `Course` represents the core catalog item
- `CourseMaterial` stores content tied to a course
- `Enrollment` links users to courses
- `CourseDto` is used for create and update requests

## Configuration

- Main application port: `8081`
- Default datasource placeholders live in `application.yml`
- Hibernate is configured for `update` mode

## Stack

- Java 17
- Spring Boot 2.7.13
- Spring Data JPA
- Spring JDBC
- MySQL
- Lombok
