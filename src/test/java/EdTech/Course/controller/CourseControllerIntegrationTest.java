package EdTech.Course.controller;

import EdTech.Course.dto.ResponseMessage;
import EdTech.Course.model.Course;
import EdTech.Course.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Test
    void getCourseByIdReturnsJsonThroughControllerLayer() throws Exception {
        Course course = new Course();
        course.setId(11L);
        course.setName("Distributed Systems");
        course.setDescription("Course on event-driven backend design");
        course.setInstructor("Bipin Verma");
        course.setAmount(4500L);

        when(courseService.getCourseResponseById(11L)).thenReturn(new EdTech.Course.dto.CourseResponse(course, 4.5));

        mockMvc.perform(get("/courses/11").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.id").value(11))
                .andExpect(jsonPath("$.course.name").value("Distributed Systems"))
                .andExpect(jsonPath("$.course.instructor").value("Bipin Verma"))
                .andExpect(jsonPath("$.course.amount").value(4500))
                .andExpect(jsonPath("$.averageRating").value(4.5));

        verify(courseService).getCourseResponseById(11L);
    }

    @Test
    void registerForCourseReturnsCreatedResponseThroughControllerLayer() throws Exception {
        when(courseService.createEnrollmentForCourse(3L, 9L, null))
                .thenReturn(new ResponseMessage("Student Enrolled Successfully"));

        mockMvc.perform(post("/courses/course/3/register/9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Student Enrolled Successfully"));

        verify(courseService).createEnrollmentForCourse(3L, 9L, null);
    }

    @Test
    void registerForCourseReturnsUnprocessableEntityWhenRuntimeExceptionThrown() throws Exception {
        when(courseService.createEnrollmentForCourse(3L, 9L, null))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/courses/course/3/register/9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(courseService).createEnrollmentForCourse(3L, 9L, null);
    }
}
