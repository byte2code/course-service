package EdTech.Course.controller;

import EdTech.Course.dto.CourseProgressDto;
import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.model.CourseProgress;
import EdTech.Course.service.CourseProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
@Tag(name = "Progress Controller", description = "Course Progress Management Endpoints")
public class CourseProgressController {

    private final CourseProgressService courseProgressService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update Course Progress", description = "Marks a course material as completed or incomplete for a user")
    public ResponseMessage updateProgress(@Valid @RequestBody CourseProgressDto progressDto) {
        courseProgressService.updateProgress(progressDto);
        return new ResponseMessage("Progress updated successfully");
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Course Progress", description = "Retrieves all progress entries for a given user")
    public List<CourseProgress> getProgressByUserId(@PathVariable Long userId) {
        return courseProgressService.getProgressByUserId(userId);
    }
}
