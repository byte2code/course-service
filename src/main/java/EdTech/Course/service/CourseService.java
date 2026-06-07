package EdTech.Course.service;

import EdTech.Course.dto.CourseDto;
import EdTech.Course.dto.Payment;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.event.CourseEventType;
import EdTech.Course.feign.PaymentService;
import EdTech.Course.feign.UserService;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.model.Enrollment;
import EdTech.Course.model.EnrollmentStatus;
import EdTech.Course.repository.CourseRepository;
import EdTech.Course.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseEventPublisher courseEventPublisher;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        Optional<Course> courseOptional = courseRepository.findById(id);
        return courseOptional.orElse(null);
    }

    public void createCourse(CourseDto courseDto) {
        Course course = new Course();
        course.setAmount(courseDto.getAmount());
        course.setName(courseDto.getName());
        course.setDescription(courseDto.getDescription());
        course.setInstructor(courseDto.getInstructor());
        for(CourseMaterial courseMaterial : courseDto.getCourseMaterial()){
            courseMaterial.setCourse(course);
        }
        for(Enrollment enrollment : courseDto.getEnrollments()){
            enrollment.setCourse(course);
        }
        course.setCourseMaterial(courseDto.getCourseMaterial());
        course.setEnrollment(courseDto.getEnrollments());
        courseRepository.save(course);
    }

    public void updateCourse(Long id, CourseDto updatedCourseDto) {
        Course existingCourse = getCourseById(id);
        if (existingCourse != null) {
            existingCourse.setName(updatedCourseDto.getName());
            existingCourse.setDescription(updatedCourseDto.getDescription());
            existingCourse.setInstructor(updatedCourseDto.getInstructor());
            existingCourse.setAmount(updatedCourseDto.getAmount());
            for(CourseMaterial courseMaterial : updatedCourseDto.getCourseMaterial()){
                courseMaterial.setCourse(existingCourse);
            }
            for(Enrollment enrollment : updatedCourseDto.getEnrollments()){
                enrollment.setCourse(existingCourse);
            }
            courseRepository.save(existingCourse);
        }
        else{
            throw new RuntimeException("Course do not exist");
        }
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public Course getCourseByName(String name) {
        return courseRepository.findByName(name);
    }

    public Course getCourseByInstructor(String instructor){
        return courseRepository.findByInstructor(instructor);
    }

    public List<CourseMaterial> getCourseMaterialByCourseId(Long id){
        return courseRepository.findById(id).orElseThrow().getCourseMaterial();
    }

    public ResponseMessage createEnrollmentForCourse(Long courseId, Long userId) {
        try {
            return createEnrollmentInternal(courseId, userId);
        } catch (DataIntegrityViolationException ex) {
            Enrollment existingEnrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> ex);
            courseEventPublisher.publishNotificationEvent(
                    existingEnrollment,
                    existingEnrollment.getCourse(),
                    CourseEventType.DUPLICATE_REQUEST,
                    "Duplicate enrollment request ignored"
            );
            return buildDuplicateResponse(existingEnrollment);
        }
    }

    private ResponseMessage createEnrollmentInternal(Long courseId, Long userId) {
        Enrollment existingEnrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (existingEnrollment != null) {
            courseEventPublisher.publishNotificationEvent(
                    existingEnrollment,
                    existingEnrollment.getCourse(),
                    CourseEventType.DUPLICATE_REQUEST,
                    "Duplicate enrollment request ignored"
            );
            return buildDuplicateResponse(existingEnrollment);
        }

        Course course = courseRepository.findById(courseId).orElseThrow();
        Enrollment enrollment = persistEnrollment(course, userId, EnrollmentStatus.INITIATED,
                "Enrollment initiated");
        courseEventPublisher.publishEnrollmentEvent(
                enrollment,
                course,
                CourseEventType.INITIATED,
                "Enrollment initiated"
        );

        try {
            // call to user to find user is available
            String token = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuIiwiaWF0IjoxNjkyNjMyNzA5LCJleHAiOjE2OTI3MTkxMDl9.6rEgbP35a-2nvXPtPDfw9mg6qLMt43DE1ElqXZcOZJ0wdtRenPEXCxYWBjwGNkG8o-B3ZxUDb431-EU0DuMqzw";
            Object object = userService.getUserById(token, userId);
            if (object == null) {
                markEnrollmentFailed(enrollment, "User not found");
                throw new RuntimeException("User not found");
            }

            markEnrollment(enrollment, EnrollmentStatus.USER_VERIFIED, "User verified through user-service");
            courseEventPublisher.publishEnrollmentEvent(
                    enrollment,
                    course,
                    CourseEventType.USER_VERIFIED,
                    "User verified through user-service"
            );
            markEnrollment(enrollment, EnrollmentStatus.PAYMENT_PENDING, "Payment request is being created");
            // creating payment
            Payment payment = new Payment();
            payment.setCourseId(courseId);
            payment.setUserId(userId);
            payment.setAmount(course.getAmount());
            courseEventPublisher.publishPaymentEvent(
                    enrollment,
                    course,
                    payment,
                    CourseEventType.REQUESTED,
                    "Payment request created for enrollment"
            );

            paymentService.createPayment(payment);

            markEnrollment(enrollment, EnrollmentStatus.ENROLLED, "Student enrolled successfully");
            courseEventPublisher.publishEnrollmentEvent(
                    enrollment,
                    course,
                    CourseEventType.CONFIRMED,
                    "Student enrolled successfully"
            );
            return new ResponseMessage("Student Enrolled Successfully");
        } catch (RuntimeException ex) {
            if (enrollment.getStatus() != EnrollmentStatus.FAILED) {
                markEnrollmentFailed(enrollment, ex.getMessage());
            }
            courseEventPublisher.publishEnrollmentEvent(
                    enrollment,
                    course,
                    CourseEventType.FAILED,
                    ex.getMessage()
            );
            courseEventPublisher.publishNotificationEvent(
                    enrollment,
                    course,
                    CourseEventType.FAILED,
                    ex.getMessage()
            );
            throw ex;
        } catch (Exception ex) {
            markEnrollmentFailed(enrollment, "Enrollment failed unexpectedly");
            courseEventPublisher.publishEnrollmentEvent(
                    enrollment,
                    course,
                    CourseEventType.FAILED,
                    "Enrollment failed unexpectedly"
            );
            courseEventPublisher.publishNotificationEvent(
                    enrollment,
                    course,
                    CourseEventType.FAILED,
                    "Enrollment failed unexpectedly"
            );
            throw new RuntimeException(ex);
        }
    }

    private ResponseMessage buildDuplicateResponse(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            return new ResponseMessage("Student already enrolled");
        }
        if (enrollment.getStatus() == EnrollmentStatus.FAILED) {
            return new ResponseMessage("Enrollment already failed");
        }
        return new ResponseMessage("Enrollment already in progress");
    }

    private Enrollment persistEnrollment(Course course, Long userId, EnrollmentStatus status, String statusMessage) {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        enrollment.setStatusMessage(statusMessage);
        return enrollmentRepository.save(enrollment);
    }

    private Enrollment markEnrollment(Enrollment enrollment, EnrollmentStatus status, String statusMessage) {
        enrollment.setStatus(status);
        enrollment.setStatusMessage(statusMessage);
        return enrollmentRepository.save(enrollment);
    }

    private Enrollment markEnrollmentFailed(Enrollment enrollment, String statusMessage) {
        return markEnrollment(enrollment, EnrollmentStatus.FAILED, statusMessage);
    }
}
