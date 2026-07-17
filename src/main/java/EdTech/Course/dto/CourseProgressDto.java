package EdTech.Course.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class CourseProgressDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Material ID is required")
    private Long materialId;

    private boolean completed;
}
