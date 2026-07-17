package EdTech.Course.controller;

import EdTech.Course.dto.CourseDto;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.service.CourseService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.PathParam;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;

@RestController
@RequestMapping("/courses")
@Tag(name = "Course Controller", description = "Course and Enrollment Management Endpoints")
public class CourseController {


    @Autowired
    private CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get All Courses", description = "Retrieves a list of all available courses")
    public List<EdTech.Course.dto.CourseResponse> getAllCourses() {
        return courseService.getAllCourseResponses();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by ID", description = "Retrieves a single course by its unique identifier")
    public EdTech.Course.dto.CourseResponse getCourseById(@PathVariable Long id) {
        return courseService.getCourseResponseById(id);
    }

    @GetMapping("/name/")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by Name", description = "Retrieves a course matching the given name")
    public Course getCourseByName(@RequestParam("name") String name) {
        return courseService.getCourseByName(name);
    }

    @GetMapping("/courseMaterial/")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course Material", description = "Retrieves all course materials for a given course ID")
    public List<CourseMaterial> getCourseMaterialByCourseId(@RequestParam("id") Long id){
        return courseService.getCourseMaterialByCourseId(id);
    }

    @GetMapping("/instructor/")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course by Instructor", description = "Retrieves a course taught by the specified instructor")
    public Course getCourseByInstructor(@RequestParam("instructor") String instructor) {
        return courseService.getCourseByInstructor(instructor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Course", description = "Creates a new course with materials and enrollment slots")
    public ResponseMessage createCourse(@Valid @RequestBody CourseDto courseDto) {
        courseService.createCourse(courseDto);
        return new ResponseMessage("Course Added Successfully");
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update Course", description = "Updates an existing course by ID")
    public ResponseMessage updateCourse(@PathVariable Long id, @Valid @RequestBody CourseDto updatedCourseDto) {
        courseService.updateCourse(id, updatedCourseDto);
        return new ResponseMessage("Course Updated Successfully");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete Course", description = "Deletes a course by ID")
    public ResponseMessage deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return new ResponseMessage("Course Deleted Successfully");
    }


    @PostMapping("/course/{courseId}/register/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "courseEnrollment", fallbackMethod = "registerForCourseFallback")
    @Operation(summary = "Register User for Course", description = "Registers a user for a specific course and triggers the enrollment state machine: INITIATED -> USER_VERIFIED -> PAYMENT_PENDING -> ENROLLED, with FAILED as a terminal state from any step")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully registered or duplicate request ignored"),
            @ApiResponse(responseCode = "422", description = "Enrollment failed — user not found, payment error, or service unavailable")
    })
    public ResponseMessage registerForCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader){
        return courseService.createEnrollmentForCourse(courseId, userId, authorizationHeader);
    }

    public ResponseMessage registerForCourseFallback(
            Long courseId,
            Long userId,
            String authorizationHeader,
            Throwable t){
        return new ResponseMessage("Services not available");
    }

    @PostMapping("/{courseId}/rate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add Rating", description = "Adds a rating and review for an enrolled course")
    public ResponseMessage addRating(@PathVariable Long courseId, @Valid @RequestBody EdTech.Course.dto.CourseRatingDto ratingDto) {
        courseService.addRating(courseId, ratingDto);
        return new ResponseMessage("Rating Added Successfully");
    }

}
