package EdTech.Course.controller;

import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.dto.CourseDto;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class courseController {

    @Autowired
    private CourseService courseService;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping("/name")
    public Course getCourseByName(@RequestParam String name) {
        return courseService.getCourseByName(name);
    }

    @GetMapping("/courseMaterial")
    public List<CourseMaterial> getCourseMaterialByCourseId(@RequestParam Long id) {
        return courseService.getCourseMaterialByCourseId(id);
    }

    @GetMapping("/instructor")
    public Course getCourseByInstructor(@RequestParam String instructor) {
        return courseService.getCourseByInstructor(instructor);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createCourse(@RequestBody CourseDto courseDto) {
        courseService.createCourse(courseDto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Course Added Successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCourse(@PathVariable Long id, @RequestBody CourseDto updatedCourseDto) {
        courseService.updateCourse(id, updatedCourseDto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Course Updated Successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Course Deleted Successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/course/{courseId}/register/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseMessage registerForCourse(@PathVariable Long courseId, @PathVariable Long userId){
        courseService.createEnrollmentForCourse(courseId, userId);
        return new ResponseMessage("Student Enrolled Successfully");
    }





}
