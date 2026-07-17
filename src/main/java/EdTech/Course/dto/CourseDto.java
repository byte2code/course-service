package EdTech.Course.dto;

import EdTech.Course.model.CourseMaterial;
import EdTech.Course.model.Enrollment;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import java.util.List;

@Getter
@Setter
@Data
public class CourseDto {
    @NotBlank(message = "Course name is required")
    private String name;
    @NotBlank(message = "Course description is required")
    private String description;
    @NotBlank(message = "Instructor is required")
    private String instructor;
    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount cannot be negative")
    private Long amount;
    private List<CourseMaterial> courseMaterial;
    private List<Enrollment> enrollments;
}
