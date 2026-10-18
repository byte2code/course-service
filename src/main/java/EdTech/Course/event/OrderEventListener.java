package EdTech.Course.event;

import EdTech.Course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final CourseService courseService;
    
    // We assume a predefined course ID for bundle purchases, e.g. 1L
    private static final Long BUNDLE_COURSE_ID = 1L;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "edtech.order.confirmed.queue", durable = "true"),
            exchange = @Exchange(value = "cnkart.order.exchange", type = "topic"),
            key = "order.confirmed"
    ))
    public void handleOrderConfirmedEvent(Map<String, Object> orderEvent) {
        log.info("Received CNKart order confirmed event: {}", orderEvent);
        
        try {
            // Extract user ID from the order event (customerId or userId)
            Object customerIdObj = orderEvent.get("customerId");
            if (customerIdObj == null) {
                customerIdObj = orderEvent.get("userId");
            }

            // Extract course ID if provided in event, otherwise fallback to BUNDLE_COURSE_ID
            Long courseId = BUNDLE_COURSE_ID;
            Object courseIdObj = orderEvent.get("courseId");
            if (courseIdObj == null) {
                courseIdObj = orderEvent.get("bundleCourseId");
            }
            if (courseIdObj != null) {
                try {
                    courseId = Long.valueOf(courseIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid courseId in order event, using default bundle course: {}", courseIdObj);
                }
            }

            if (customerIdObj != null) {
                Long userId = Long.valueOf(customerIdObj.toString());
                log.info("Triggering automatic enrollment for user: {} in course: {}", userId, courseId);
                courseService.createEnrollmentForCourse(courseId, userId);
            } else {
                log.warn("Cannot process order event: missing customerId/userId in payload: {}", orderEvent);
            }
        } catch (RuntimeException e) {
            log.error("Failed to process order event: {}", orderEvent, e);
            throw e; // Rethrow to ensure AMQP message retry / DLQ routing instead of silent drop
        }
    }
}
