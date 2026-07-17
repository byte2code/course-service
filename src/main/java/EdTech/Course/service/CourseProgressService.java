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

        List<CourseProgress> userProgress = courseProgressRepository.findByUserId(userId);
        
        long completedCount = userProgress.stream()
                .filter(CourseProgress::isCompleted)
                .filter(p -> {
                    CourseMaterial mat = courseMaterialRepository.findById(p.getMaterialId()).orElse(null);
                    return mat != null && mat.getCourse() != null && mat.getCourse().getId().equals(course.getId());
                })
                .count();

        if (completedCount == totalMaterials) {
            courseEventPublisher.publishCertificateEvent(userId, course, "Course completed and certificate earned.");
        }
    }

    public List<CourseProgress> getProgressByUserId(Long userId) {
        return courseProgressRepository.findByUserId(userId);
    }
}
