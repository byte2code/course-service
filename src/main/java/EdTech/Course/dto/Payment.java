package EdTech.Course.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long userId;
    private Long courseId;
    private String date;
    private Long amount;
}
