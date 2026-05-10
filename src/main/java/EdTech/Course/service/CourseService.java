package EdTech.Course.service;

import EdTech.Course.dto.CourseDto;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.model.Enrollment;
import EdTech.Course.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    public Course getCourseByName(String name) {
        return courseRepository.findByName(name);
    }

    public List<CourseMaterial> getCourseMaterialByCourseId(Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null && course.getCourseMaterial() != null) {
            return course.getCourseMaterial();
        }
        return new ArrayList<>();
    }

    public Course getCourseByInstructor(String instructor) {
        return courseRepository.findByInstructor(instructor);
    }

    public void createCourse(CourseDto courseDto) {
        Course course = new Course();
        course.setName(courseDto.getName());
        course.setDescription(courseDto.getDescription());
        course.setInstructor(courseDto.getInstructor());
        course.setAmount(courseDto.getAmount());

        // Map and set course materials
        List<CourseMaterial> materials = new ArrayList<>();
        if (courseDto.getCourseMaterial() != null) {
            materials = courseDto.getCourseMaterial()
                .stream()
                .map(cmDto -> {
                    CourseMaterial cm = new CourseMaterial();
                    cm.setType(cmDto.getType());
                    cm.setDescription(cmDto.getDescription());
                    cm.setCourse(course); // set relationship
                    return cm;
                }).collect(Collectors.toList());
        }
        course.setCourseMaterial(materials);

        // Map and set enrollments
        List<Enrollment> enrollments = new ArrayList<>();
        if (courseDto.getEnrollments() != null) {
            enrollments = courseDto.getEnrollments()
                .stream()
                .map(enDto -> {
                    Enrollment en = new Enrollment();
                    en.setUserId(enDto.getUserId());
                    en.setCourse(course);
                    return en;
                }).collect(Collectors.toList());
        }
        course.setEnrollment(enrollments);

        courseRepository.save(course);
    }

    public void updateCourse(Long id, CourseDto updatedCourseDto) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            course.setName(updatedCourseDto.getName());
            course.setDescription(updatedCourseDto.getDescription());
            course.setInstructor(updatedCourseDto.getInstructor());
            course.setAmount(updatedCourseDto.getAmount());

            // Update course materials (replace all)
            List<CourseMaterial> updatedMaterials = new ArrayList<>();
            if (updatedCourseDto.getCourseMaterial() != null) {
                updatedMaterials = updatedCourseDto.getCourseMaterial()
                    .stream()
                    .map(cmDto -> {
                        CourseMaterial cm = new CourseMaterial();
                        cm.setType(cmDto.getType());
                        cm.setDescription(cmDto.getDescription());
                        cm.setCourse(course);
                        return cm;
                    }).collect(Collectors.toList());
            }
            course.setCourseMaterial(updatedMaterials);

            // Update enrollments similarly
            List<Enrollment> updatedEnrollments = new ArrayList<>();
            if (updatedCourseDto.getEnrollments() != null) {
                updatedEnrollments = updatedCourseDto.getEnrollments()
                    .stream()
                    .map(enDto -> {
                        Enrollment en = new Enrollment();
                        en.setUserId(enDto.getUserId());
                        en.setCourse(course);
                        return en;
                    }).collect(Collectors.toList());
            }
            course.setEnrollment(updatedEnrollments);

            courseRepository.save(course);
        }
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}
