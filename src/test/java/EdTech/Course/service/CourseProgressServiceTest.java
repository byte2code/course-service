package EdTech.Course.service;

import EdTech.Course.dto.CourseProgressDto;
import EdTech.Course.model.Certificate;
import EdTech.Course.model.Course;
import EdTech.Course.model.CourseMaterial;
import EdTech.Course.model.CourseProgress;
import EdTech.Course.repository.CertificateRepository;
import EdTech.Course.repository.CourseMaterialRepository;
import EdTech.Course.repository.CourseProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseProgressServiceTest {

    @Mock
    private CourseProgressRepository courseProgressRepository;

    @Mock
    private CourseMaterialRepository courseMaterialRepository;

    @Mock
    private CourseEventPublisher courseEventPublisher;

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private CourseProgressService courseProgressService;

    @Test
    void updateProgressSavesCompletedProgressAndIssuesCertificateWhenAllMaterialsCompleted() {
        CourseProgressDto dto = new CourseProgressDto();
        dto.setUserId(10L);
        dto.setMaterialId(101L);
        dto.setCompleted(true);

        Course course = new Course();
        course.setId(5L);
        CourseMaterial material = new CourseMaterial();
        material.setId(101L);
        material.setCourse(course);
        course.setCourseMaterial(List.of(material));

        when(courseProgressRepository.findByUserIdAndMaterialId(10L, 101L)).thenReturn(Optional.empty());
        when(courseMaterialRepository.findById(101L)).thenReturn(Optional.of(material));
        when(certificateRepository.existsByUserIdAndCourseId(10L, 5L)).thenReturn(false);
        when(courseProgressRepository.countCompletedMaterialsByUserIdAndCourseId(10L, 5L)).thenReturn(1L);

        courseProgressService.updateProgress(dto);

        verify(courseProgressRepository).save(any(CourseProgress.class));
        verify(certificateRepository).save(any(Certificate.class));
        verify(courseEventPublisher).publishCertificateEvent(eq(10L), eq(course), eq("Course completed and certificate earned."));
    }

    @Test
    void updateProgressDoesNotIssueDuplicateCertificateIfAlreadyIssued() {
        CourseProgressDto dto = new CourseProgressDto();
        dto.setUserId(10L);
        dto.setMaterialId(101L);
        dto.setCompleted(true);

        Course course = new Course();
        course.setId(5L);
        CourseMaterial material = new CourseMaterial();
        material.setId(101L);
        material.setCourse(course);
        course.setCourseMaterial(List.of(material));

        when(courseProgressRepository.findByUserIdAndMaterialId(10L, 101L)).thenReturn(Optional.empty());
        when(courseMaterialRepository.findById(101L)).thenReturn(Optional.of(material));
        when(certificateRepository.existsByUserIdAndCourseId(10L, 5L)).thenReturn(true); // Already issued

        courseProgressService.updateProgress(dto);

        verify(courseProgressRepository).save(any(CourseProgress.class));
        verify(certificateRepository, never()).save(any(Certificate.class));
        verify(courseEventPublisher, never()).publishCertificateEvent(any(), any(), any());
    }
}
