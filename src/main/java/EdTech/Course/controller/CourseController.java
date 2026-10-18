package EdTech.Course.controller;

import EdTech.Course.dto.CourseDto;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.service.CourseService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Controller", description = "Course and Enrollment Management Endpoints")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get All Courses", description = "Retrieves a list of all available courses (supports optional pagination with page and size parameters)")
    public List<EdTech.Course.dto.CourseResponse> getAllCourses(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        if (page != null && size != null) {
            return courseService.getCourseResponsesPaged(page, size);
        }
        return courseService.getAllCourseResponses();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by ID", description = "Retrieves a single course by its unique identifier")
    public EdTech.Course.dto.CourseResponse getCourseById(@PathVariable Long id) {
        return courseService.getCourseResponseById(id);
    }

    @GetMapping(value = {"/name", "/name/"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by Name", description = "Retrieves a course matching the given name")
    public Course getCourseByName(@RequestParam("name") String name) {
        return courseService.getCourseByName(name);
    }

    @GetMapping(value = {"/courseMaterial", "/courseMaterial/", "/{id}/materials"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course Material", description = "Retrieves all course materials for a given course ID")
    public List<CourseMaterial> getCourseMaterialByCourseId(
            @RequestParam(value = "id", required = false) Long paramId,
            @PathVariable(value = "id", required = false) Long pathId){
        Long effectiveId = pathId != null ? pathId : paramId;
        if (effectiveId == null) {
            throw new IllegalArgumentException("Course ID must be provided");
        }
        return courseService.getCourseMaterialByCourseId(effectiveId);
    }

    @GetMapping(value = {"/instructor", "/instructor/"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by Instructor", description = "Retrieves a course taught by the specified instructor")
    public Course getCourseByInstructor(@RequestParam("instructor") String instructor) {
        return courseService.getCourseByInstructor(instructor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Create Course", description = "Creates a new course with materials and enrollment slots")
    public ResponseMessage createCourse(@Valid @RequestBody CourseDto courseDto) {
        courseService.createCourse(courseDto);
        return new ResponseMessage("Course Added Successfully");
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    @Operation(summary = "Update Course", description = "Updates an existing course by ID")
    public ResponseMessage updateCourse(@PathVariable Long id, @Valid @RequestBody CourseDto updatedCourseDto) {
        courseService.updateCourse(id, updatedCourseDto);
        return new ResponseMessage("Course Updated Successfully");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Course", description = "Deletes a course by ID")
    public ResponseMessage deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return new ResponseMessage("Course Deleted Successfully");
    }


    @PostMapping("/course/{courseId}/register/{userId}")
    @CircuitBreaker(name = "courseEnrollment", fallbackMethod = "registerForCourseFallback")
    @Operation(summary = "Register User for Course", description = "Registers a user for a specific course and triggers the enrollment state machine: INITIATED -> USER_VERIFIED -> PAYMENT_PENDING -> ENROLLED, with FAILED as a terminal state from any step")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully registered or duplicate request ignored"),
            @ApiResponse(responseCode = "422", description = "Enrollment failed — user not found, payment error, or invalid state"),
            @ApiResponse(responseCode = "503", description = "Downstream services unavailable (circuit breaker fallback)")
    })
    public ResponseEntity<ResponseMessage> registerForCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader){
        ResponseMessage response = courseService.createEnrollmentForCourse(courseId, userId, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<ResponseMessage> registerForCourseFallback(
            Long courseId,
            Long userId,
            String authorizationHeader,
            Throwable t){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ResponseMessage("Services not available: " + (t != null ? t.getMessage() : "downstream failure")));
    }

    @PostMapping("/{courseId}/rate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add Rating", description = "Adds a rating and review for an enrolled course")
    public ResponseMessage addRating(@PathVariable Long courseId, @Valid @RequestBody EdTech.Course.dto.CourseRatingDto ratingDto) {
        courseService.addRating(courseId, ratingDto);
        return new ResponseMessage("Rating Added Successfully");
    }

}
