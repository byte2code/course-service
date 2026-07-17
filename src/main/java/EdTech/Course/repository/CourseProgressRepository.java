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
}
