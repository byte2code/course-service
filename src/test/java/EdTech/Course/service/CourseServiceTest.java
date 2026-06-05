package EdTech.Course.service;

import EdTech.Course.dto.Payment;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.feign.PaymentService;
import EdTech.Course.feign.UserService;
import EdTech.Course.model.Course;
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
        when(userService.getUserById(anyString(), eq(2L))).thenReturn(new Object());
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
    }

    @Test
    void createEnrollmentForCourseMarksFailedWhenPaymentFails() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(anyString(), eq(2L))).thenReturn(new Object());
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
    }

    @Test
    void createEnrollmentForCourseMarksFailedWhenUserLookupFails() {
        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(anyString(), eq(2L))).thenReturn(null);
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
    }
}
