package EdTech.Course.service;

import EdTech.Course.dto.Payment;
import EdTech.Course.event.CourseEvent;
import EdTech.Course.event.CourseEventCategory;
import EdTech.Course.event.CourseEventType;
import EdTech.Course.model.Course;
import EdTech.Course.model.Enrollment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CourseEventPublisher {

    private static final String EXCHANGE = "course.lifecycle.exchange";
    private static final Logger log = LoggerFactory.getLogger(CourseEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public CourseEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishEnrollmentEvent(Enrollment enrollment, Course course, CourseEventType type, String message) {
        publish(buildEvent(CourseEventCategory.ENROLLMENT, type, enrollment, course, null, message));
    }

    public void publishPaymentEvent(Enrollment enrollment, Course course, Payment payment, CourseEventType type, String message) {
        publish(buildEvent(CourseEventCategory.PAYMENT, type, enrollment, course, payment.getAmount(), message));
    }

    public void publishNotificationEvent(Enrollment enrollment, Course course, CourseEventType type, String message) {
        publish(buildEvent(CourseEventCategory.NOTIFICATION, type, enrollment, course, null, message));
    }

    public void publishCertificateEvent(Long userId, Course course, String message) {
        publish(CourseEvent.builder()
                .category(CourseEventCategory.CERTIFICATE)
                .type(CourseEventType.EARNED)
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .userId(userId)
                .message(message)
                .correlationId((course != null ? course.getId() : null) + ":" + userId)
                .eventTime(Instant.now().toString())
                .build());
    }

    private CourseEvent buildEvent(CourseEventCategory category,
                                   CourseEventType type,
                                   Enrollment enrollment,
                                   Course course,
                                   Long amount,
                                   String message) {
        return CourseEvent.builder()
                .category(category)
                .type(type)
                .enrollmentId(enrollment != null ? enrollment.getId() : null)
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .userId(enrollment != null ? enrollment.getUserId() : null)
                .enrollmentStatus(enrollment != null && enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .amount(amount)
                .message(message)
                .correlationId(buildCorrelationId(enrollment))
                .eventTime(Instant.now().toString())
                .build();
    }

    private void publish(CourseEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, buildRoutingKey(event), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialize course event {}", event, ex);
        } catch (RuntimeException ex) {
            log.warn("Unable to publish course event {}", event, ex);
        }
    }

    private String buildRoutingKey(CourseEvent event) {
        return "course." + event.getCategory().name().toLowerCase() + "." + event.getType().name().toLowerCase().replace('_', '.');
    }

    private String buildCorrelationId(Enrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null || enrollment.getUserId() == null) {
            return null;
        }
        return enrollment.getCourse().getId() + ":" + enrollment.getUserId();
    }
}
