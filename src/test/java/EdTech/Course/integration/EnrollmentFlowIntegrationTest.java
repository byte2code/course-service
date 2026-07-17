package EdTech.Course.integration;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class EnrollmentFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("course_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("eureka.client.enabled", () -> false);
        registry.add("spring.cloud.discovery.enabled", () -> false);
    }

    @TestConfiguration
    static class SecurityOverrideConfig {
        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return (web) -> web.ignoring().antMatchers("/**");
        }
    }

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockBean
    private UserService userService;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    void enrollmentHappyPath() {
        // save a Course
        Course course = new Course();
        course.setName("Integration Testing Course");
        course.setAmount(100L);
        course = courseRepository.save(course);

        // mock userService and paymentService
        when(userService.getUserById(any(), any())).thenReturn(new Object());
        
        // POST to /courses/course/{courseId}/register/1
        ResponseEntity<ResponseMessage> response = restTemplate.postForEntity(
                "/courses/course/" + course.getId() + "/register/1", null, ResponseMessage.class);

        // assert response message is "Student Enrolled Successfully"
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Student Enrolled Successfully", response.getBody().getMessage());

        // assert enrollment in DB has status ENROLLED
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        assertEquals(1, enrollments.size());
        assertEquals(EnrollmentStatus.ENROLLED, enrollments.get(0).getStatus());
    }

    @Test
    void duplicateEnrollmentReturnsSameState() {
        // save a Course
        Course course = new Course();
        course.setName("Integration Testing 2");
        course.setAmount(100L);
        course = courseRepository.save(course);

        when(userService.getUserById(any(), any())).thenReturn(new Object());

        // call the same endpoint twice
        String url = "/courses/course/" + course.getId() + "/register/1";
        ResponseEntity<ResponseMessage> response1 = restTemplate.postForEntity(url, null, ResponseMessage.class);
        ResponseEntity<ResponseMessage> response2 = restTemplate.postForEntity(url, null, ResponseMessage.class);

        // assert count is 1
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        assertEquals(1, enrollments.size());

        // assert second response contains "already"
        assertTrue(response2.getBody().getMessage().toLowerCase().contains("already"));
    }
}
