package EdTech.Course.repository;

import EdTech.Course.model.CourseRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRatingRepository extends JpaRepository<CourseRating, Long> {
    
    Optional<CourseRating> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT AVG(r.rating) FROM CourseRating r WHERE r.courseId = :courseId")
    Double getAverageRatingByCourseId(@Param("courseId") Long courseId);
}
