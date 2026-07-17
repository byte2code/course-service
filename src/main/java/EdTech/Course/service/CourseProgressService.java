package EdTech.Course.service;

import EdTech.Course.dto.CourseProgressDto;
import EdTech.Course.model.CourseProgress;
import EdTech.Course.repository.CourseProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseProgressService {

    private final CourseProgressRepository courseProgressRepository;

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
    }

    public List<CourseProgress> getProgressByUserId(Long userId) {
        return courseProgressRepository.findByUserId(userId);
    }
}
