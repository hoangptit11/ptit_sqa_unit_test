package ptit.com.enghub.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ptit.com.enghub.dto.request.CourseRequest;
import ptit.com.enghub.dto.response.CourseResponse;
import ptit.com.enghub.entity.Language;
import ptit.com.enghub.repository.CourseRepository;
import ptit.com.enghub.repository.LanguageRepository;
import ptit.com.enghub.service.IService.CourseService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LanguageRepository languageRepository;

    private Long languageId;

    @BeforeEach
    void setUp() {
        Language language = new Language();
        language.setCode("EN");
        language.setName("English");
        languageId = languageRepository.save(language).getId();
    }

    @Test
    void createCourse_validData_TC_CS_005() {
        // TC-CS-005: Valid request should create one course row.
        CourseRequest request = new CourseRequest();
        request.setTitle("Spanish Basics");
        request.setDescription("Learn Spanish from scratch");
        request.setLevel("BEGINNER");
        request.setLanguageId(languageId);

        CourseResponse response = courseService.createCourse(request);

        // CheckDB: verify insert happened.
        assertNotNull(response.getId());
        assertTrue(courseRepository.existsById(response.getId()));
    }

    @Test
    void createCourse_nonExistingLanguage_TC_CS_007() {
        // TC-CS-007: creating with invalid language must fail.
        CourseRequest request = new CourseRequest();
        request.setTitle("French Course");
        request.setDescription("Learn French");
        request.setLevel("BEGINNER");
        request.setLanguageId(99999L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> courseService.createCourse(request));
        assertTrue(ex.getMessage().contains("Language"));
    }

    @Test
    void getCourseById_notFound_TC_CS_004() {
        // TC-CS-004: unknown id should throw "Course not found".
        RuntimeException ex = assertThrows(RuntimeException.class, () -> courseService.getCourseById(99999L));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void updateCourse_existing_TC_CS_009() {
        // TC-CS-009: update should persist changed title and level.
        CourseRequest create = new CourseRequest();
        create.setTitle("Old Title");
        create.setDescription("Old Desc");
        create.setLevel("BEGINNER");
        create.setLanguageId(languageId);
        CourseResponse created = courseService.createCourse(create);

        CourseRequest update = new CourseRequest();
        update.setTitle("New Title");
        update.setDescription("New Desc");
        update.setLevel("INTERMEDIATE");
        update.setLanguageId(languageId);
        CourseResponse updated = courseService.updateCourse(created.getId(), update);

        // CheckDB: updated values are returned from persistence layer.
        assertEquals("New Title", updated.getTitle());
        assertEquals("INTERMEDIATE", updated.getLevel());
    }

    @Test
    void deleteCourse_thenNotFound_TC_CS_013() {
        // TC-CS-013: deleting existing course should remove it.
        CourseRequest request = new CourseRequest();
        request.setTitle("To Delete");
        request.setDescription("Tmp");
        request.setLevel("BEGINNER");
        request.setLanguageId(languageId);
        CourseResponse created = courseService.createCourse(request);

        courseService.deleteCourse(created.getId());

        // CheckDB: row should not exist in current transaction.
        assertFalse(courseRepository.existsById(created.getId()));
        assertThrows(RuntimeException.class, () -> courseService.getCourseById(created.getId()));
    }
}
