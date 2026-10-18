package EdTech.Course.repository;

import EdTech.Course.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
