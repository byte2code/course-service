package EdTech.Course.repository;

import EdTech.Course.model.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    List<CourseProgress> findByUserId(Long userId);
    Optional<CourseProgress> findByUserIdAndMaterialId(Long userId, Long materialId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cp) FROM CourseProgress cp, CourseMaterial cm " +
           "WHERE cp.materialId = cm.id AND cm.course.id = :courseId " +
           "AND cp.userId = :userId AND cp.completed = true")
    long countCompletedMaterialsByUserIdAndCourseId(@org.springframework.data.repository.query.Param("userId") Long userId,
                                                    @org.springframework.data.repository.query.Param("courseId") Long courseId);
}
