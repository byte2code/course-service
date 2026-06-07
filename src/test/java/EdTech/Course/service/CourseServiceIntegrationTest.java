package EdTech.Course.service;

import EdTech.Course.dto.Payment;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.event.CourseEventType;
import EdTech.Course.feign.PaymentService;
import EdTech.Course.feign.UserService;
import EdTech.Course.model.Course;
import EdTech.Course.model.Enrollment;
import EdTech.Course.model.EnrollmentStatus;
import EdTech.Course.repository.CourseRepository;
import EdTech.Course.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = CourseServiceIntegrationTest.TestConfig.class,
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration," +
                        "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration," +
                        "org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistryAutoConfiguration",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false"
        }
)
class CourseServiceIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CourseService.class)
    static class TestConfig {
    }

    @Autowired
    private CourseService courseService;

    @MockBean
    private CourseRepository courseRepository;

    @MockBean
    private EnrollmentRepository enrollmentRepository;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserService userService;

    @MockBean
    private CourseEventPublisher courseEventPublisher;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void springContextWiresEnrollmentFlowAndPublishesLifecycleEvents() {
        assertNotNull(courseService);

        Course course = new Course();
        course.setId(1L);
        course.setAmount(2500L);
        course.setName("Course Service");

        List<EnrollmentStatus> states = new ArrayList<>();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());
        when(userService.getUserById(anyString(), eq(2L))).thenReturn(new Object());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            states.add(enrollment.getStatus());
            if (enrollment.getId() == null) {
                enrollment.setId(101L);
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
        verify(courseEventPublisher).publishEnrollmentEvent(
                any(Enrollment.class),
                eq(course),
                eq(CourseEventType.INITIATED),
                eq("Enrollment initiated")
        );
        verify(courseEventPublisher).publishPaymentEvent(
                any(Enrollment.class),
                eq(course),
                any(Payment.class),
                eq(CourseEventType.REQUESTED),
                eq("Payment request created for enrollment")
        );
        verify(courseEventPublisher).publishEnrollmentEvent(
                any(Enrollment.class),
                eq(course),
                eq(CourseEventType.CONFIRMED),
                eq("Student enrolled successfully")
        );
    }

    @Test
    void springContextReturnsExistingEnrollmentForDuplicateRequest() {
        Course course = new Course();
        course.setId(1L);
        course.setName("Course Service");

        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setId(21L);
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
