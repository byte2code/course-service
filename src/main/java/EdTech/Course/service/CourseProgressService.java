package EdTech.Course.service;

import EdTech.Course.dto.CourseProgressDto;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.model.CourseProgress;
import EdTech.Course.repository.CourseMaterialRepository;
import EdTech.Course.repository.CourseProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseProgressService {

    private final CourseProgressRepository courseProgressRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseEventPublisher courseEventPublisher;
    private final EdTech.Course.repository.CertificateRepository certificateRepository;

    public void updateProgress(CourseProgressDto progressDto) {
        Optional<CourseProgress> existingProgress = courseProgressRepository
                .findByUserIdAndMaterialId(progressDto.getUserId(), progressDto.getMaterialId());

        CourseProgress progress = existingProgress.orElseGet(() -> {
            CourseProgress newProgress = new CourseProgress();
            newProgress.setUserId(progressDto.getUserId());
            newProgress.setMaterialId(progressDto.getMaterialId());
            return newProgress;
        });

        progress.setCompleted(progressDto.isCompleted());
        if (!progress.isCompleted()) {
            progress.setCompletedAt(null); // Reset if unmarked
        }

        courseProgressRepository.save(progress);

        if (progress.isCompleted()) {
            checkAndIssueCertificate(progressDto.getUserId(), progressDto.getMaterialId());
        }
    }

    private void checkAndIssueCertificate(Long userId, Long materialId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId).orElse(null);
        if (material == null || material.getCourse() == null) return;

        Course course = material.getCourse();
        int totalMaterials = course.getCourseMaterial() != null ? course.getCourseMaterial().size() : 0;
        if (totalMaterials == 0) return;

        // Idempotency: Don't re-issue certificate if already earned
        if (certificateRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            return;
        }

        long completedCount = courseProgressRepository.countCompletedMaterialsByUserIdAndCourseId(userId, course.getId());

        if (completedCount == totalMaterials) {
            EdTech.Course.model.Certificate certificate = new EdTech.Course.model.Certificate();
            certificate.setUserId(userId);
            certificate.setCourseId(course.getId());
            certificate.setIssuedAt(java.time.LocalDateTime.now());
            certificateRepository.save(certificate);

            courseEventPublisher.publishCertificateEvent(userId, course, "Course completed and certificate earned.");
        }
    }

    public List<CourseProgress> getProgressByUserId(Long userId) {
        return courseProgressRepository.findByUserId(userId);
    }
}
