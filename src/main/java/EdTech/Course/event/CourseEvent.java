package EdTech.Course.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvent {
    private CourseEventCategory category;
    private CourseEventType type;
    private Long enrollmentId;
    private Long courseId;
    private String courseName;
    private Long userId;
    private String enrollmentStatus;
    private Long amount;
    private String message;
    private String correlationId;
    private String eventTime;
}
