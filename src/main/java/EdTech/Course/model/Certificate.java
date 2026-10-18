package EdTech.Course.model;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_certificate", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    public void onPrePersist() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }
}
