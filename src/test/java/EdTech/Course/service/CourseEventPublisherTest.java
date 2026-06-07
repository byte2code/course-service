package EdTech.Course.service;

import EdTech.Course.dto.Payment;
import EdTech.Course.event.CourseEventType;
import EdTech.Course.model.Course;
import EdTech.Course.model.Enrollment;
import EdTech.Course.model.EnrollmentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CourseEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final CourseEventPublisher courseEventPublisher = new CourseEventPublisher(rabbitTemplate, new ObjectMapper());

    @Test
    void publishPaymentEventSendsJsonPayloadToLifecycleExchange() throws Exception {
        Course course = new Course();
        course.setId(7L);
        course.setName("Spring Boot Masterclass");

        Enrollment enrollment = new Enrollment();
        enrollment.setId(11L);
        enrollment.setUserId(42L);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.PAYMENT_PENDING);

        Payment payment = new Payment();
        payment.setAmount(1500L);

        courseEventPublisher.publishPaymentEvent(
                enrollment,
                course,
                payment,
                CourseEventType.REQUESTED,
                "Payment request created for enrollment"
        );

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                payloadCaptor.capture()
        );

        assertTrue(exchangeCaptor.getValue().equals("course.lifecycle.exchange"));
        assertTrue(routingKeyCaptor.getValue().equals("course.payment.requested"));
        assertTrue(payloadCaptor.getValue().contains("\"category\":\"PAYMENT\""));
        assertTrue(payloadCaptor.getValue().contains("\"type\":\"REQUESTED\""));
        assertTrue(payloadCaptor.getValue().contains("\"courseId\":7"));
        assertTrue(payloadCaptor.getValue().contains("\"userId\":42"));
        assertTrue(payloadCaptor.getValue().contains("\"amount\":1500"));
    }
}
