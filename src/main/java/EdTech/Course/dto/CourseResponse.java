package EdTech.Course.dto;

import EdTech.Course.model.Course;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CourseResponse {
    private Course course;
    private Double averageRating;

    public CourseResponse(Course course, Double averageRating) {
        this.course = course;
        this.averageRating = averageRating;
    }
}
