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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PaymentService paymentService;

    @Mock
    private UserService userService;

    @Mock
    private CourseEventPublisher courseEventPublisher;

    @Mock
    private EdTech.Course.repository.CourseRatingRepository courseRatingRepository;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        // Mockito injects the mocks into CourseService.
    }

    @Test
    void createEnrollmentForCourseAdvancesThroughAllSuccessStates() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(any(), eq(2L))).thenReturn(new Object());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            states.add(enrollment.getStatus());
            if (enrollment.getId() == null) {
                enrollment.setId(99L);
            }
            return enrollment;
        });

        ResponseMessage response = courseService.createEnrollmentForCourse(1L, 2L);

        assertEquals("Student Enrolled Successfully", response.getMessage());
        assertEquals(List.of(
                EnrollmentStatus.INITIATED,
                EnrollmentStatus.USER_VERIFIED,
                EnrollmentStatus.PAYMENT_PENDING,
                EnrollmentStatus.ENROLLED
        ), states);
        verify(paymentService).createPayment(any(Payment.class));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.INITIATED), eq("Enrollment initiated"));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.USER_VERIFIED), eq("User verified through user-service"));
        verify(courseEventPublisher).publishPaymentEvent(any(Enrollment.class), eq(course), any(Payment.class), eq(CourseEventType.REQUESTED), eq("Payment request created for enrollment"));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.CONFIRMED), eq("Student enrolled successfully"));
    }

    @Test
    void createEnrollmentForCourseMarksFailedWhenPaymentFails() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(any(), eq(2L))).thenReturn(new Object());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            states.add(enrollment.getStatus());
            if (enrollment.getId() == null) {
                enrollment.setId(99L);
            }
            return enrollment;
        });
        doThrow(new RuntimeException("payment unavailable")).when(paymentService).createPayment(any(Payment.class));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.createEnrollmentForCourse(1L, 2L));

        assertEquals("payment unavailable", exception.getMessage());
        assertEquals(List.of(
                EnrollmentStatus.INITIATED,
                EnrollmentStatus.USER_VERIFIED,
                EnrollmentStatus.PAYMENT_PENDING,
                EnrollmentStatus.FAILED
        ), states);
        verify(paymentService).createPayment(any(Payment.class));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.INITIATED), eq("Enrollment initiated"));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.USER_VERIFIED), eq("User verified through user-service"));
        verify(courseEventPublisher).publishPaymentEvent(any(Enrollment.class), eq(course), any(Payment.class), eq(CourseEventType.REQUESTED), eq("Payment request created for enrollment"));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.FAILED), anyString());
        verify(courseEventPublisher).publishNotificationEvent(any(Enrollment.class), eq(course), eq(CourseEventType.FAILED), anyString());
    }

    @Test
    void createEnrollmentForCourseMarksFailedWhenUserLookupFails() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(any(), eq(2L))).thenReturn(null);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            states.add(enrollment.getStatus());
            if (enrollment.getId() == null) {
                enrollment.setId(99L);
            }
            return enrollment;
        });

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> courseService.createEnrollmentForCourse(1L, 2L));

        assertEquals("User not found", exception.getMessage());
        assertEquals(List.of(
                EnrollmentStatus.INITIATED,
                EnrollmentStatus.FAILED
        ), states);
        verify(paymentService, never()).createPayment(any(Payment.class));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.INITIATED), eq("Enrollment initiated"));
        verify(courseEventPublisher).publishEnrollmentEvent(any(Enrollment.class), eq(course), eq(CourseEventType.FAILED), anyString());
        verify(courseEventPublisher).publishNotificationEvent(any(Enrollment.class), eq(course), eq(CourseEventType.FAILED), anyString());
    }

    @Test
    void createEnrollmentForCourseReturnsExistingEnrollmentWithoutDuplicatePayment() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setId(10L);
        existingEnrollment.setUserId(2L);
        existingEnrollment.setCourse(course);
        existingEnrollment.setStatus(EnrollmentStatus.ENROLLED);
        existingEnrollment.setStatusMessage("Student enrolled successfully");

        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.of(existingEnrollment));

        ResponseMessage response = courseService.createEnrollmentForCourse(1L, 2L);

        assertEquals("Student already enrolled", response.getMessage());
        verify(courseRepository, never()).findById(anyLong());
        verify(userService, never()).getUserById(anyString(), anyLong());
        verify(paymentService, never()).createPayment(any(Payment.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(courseEventPublisher).publishNotificationEvent(any(Enrollment.class), eq(course), eq(CourseEventType.DUPLICATE_REQUEST), eq("Duplicate enrollment request ignored"));
    }

    @Test
    void getAllCoursesReturnsList() {
        List<Course> courses = List.of(new Course(), new Course());
        when(courseRepository.findAll()).thenReturn(courses);
        List<Course> result = courseService.getAllCourses();
        assertEquals(2, result.size());
    }

    @Test
    void getCourseByIdReturnsCourse() {
        Course course = new Course();
        course.setId(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        Course result = courseService.getCourseById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void createCourseSavesCourse() {
        CourseDto dto = new CourseDto();
        dto.setName("Test");
        dto.setCourseMaterial(new ArrayList<>());
        dto.setEnrollments(new ArrayList<>());
        courseService.createCourse(dto);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void updateCourseUpdatesExistingCourse() {
        Course course = new Course();
        course.setId(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        
        CourseDto dto = new CourseDto();
        dto.setName("Updated");
        dto.setCourseMaterial(new ArrayList<>());
        dto.setEnrollments(new ArrayList<>());
        courseService.updateCourse(1L, dto);
        
        verify(courseRepository).save(course);
        assertEquals("Updated", course.getName());
    }

    @Test
    void updateCourseThrowsExceptionIfNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());
        
        CourseDto dto = new CourseDto();
        assertThrows(RuntimeException.class, () -> courseService.updateCourse(1L, dto));
    }

    @Test
    void deleteCourseDeletesById() {
        courseService.deleteCourse(1L);
        verify(courseRepository).deleteById(1L);
    }

    @Test
    void getCourseByNameReturnsCourse() {
        Course course = new Course();
        course.setName("Java");
        when(courseRepository.findByName("Java")).thenReturn(course);
        Course result = courseService.getCourseByName("Java");
        assertNotNull(result);
        assertEquals("Java", result.getName());
    }

    @Test
    void getCourseByInstructorReturnsCourse() {
        Course course = new Course();
        course.setInstructor("Bob");
        when(courseRepository.findByInstructor("Bob")).thenReturn(course);
        Course result = courseService.getCourseByInstructor("Bob");
        assertNotNull(result);
        assertEquals("Bob", result.getInstructor());
    }

    @Test
    void getCourseMaterialByCourseIdReturnsMaterials() {
        Course course = new Course();
        course.setCourseMaterial(new ArrayList<>());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        
        List<CourseMaterial> materials = courseService.getCourseMaterialByCourseId(1L);
        assertNotNull(materials);
    }
}
