package ptit.com.enghub.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ptit.com.enghub.dto.request.CourseRequest;
import ptit.com.enghub.dto.request.UnitRequest;
import ptit.com.enghub.dto.response.CourseResponse;
import ptit.com.enghub.dto.response.UnitResponse;
import ptit.com.enghub.entity.Language;
import ptit.com.enghub.repository.LanguageRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.service.IService.CourseService;
import ptit.com.enghub.service.IService.UnitService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnitServiceTest {

    @Autowired
    private UnitService unitService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private LanguageRepository languageRepository;

    private Long courseId;

    @BeforeEach
    void setUp() {
        Language language = new Language();
        language.setCode("EN");
        language.setName("English");
        Long languageId = languageRepository.save(language).getId();

        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setTitle("Course for Unit Tests");
        courseRequest.setDescription("Description");
        courseRequest.setLevel("BEGINNER");
        courseRequest.setLanguageId(languageId);
        CourseResponse courseResponse = courseService.createCourse(courseRequest);
        courseId = courseResponse.getId();
    }

    @Test
    void createUnit_validData_TC_US_004() {
        // TC-US-004: valid unit should be created and linked with course.
        UnitRequest request = new UnitRequest();
        request.setTitle("Unit 1");
        request.setDescription("Unit description");
        request.setOrderIndex(1);
        request.setCourseId(courseId);

        UnitResponse response = unitService.createUnit(request);

        // CheckDB: unit row exists after create.
        assertNotNull(response.getId());
        assertTrue(unitRepository.existsById(response.getId()));
    }

    @Test
    void createUnit_courseNotFound_TC_US_007() {
        // TC-US-007: invalid courseId should fail.
        UnitRequest request = new UnitRequest();
        request.setTitle("Unit");
        request.setOrderIndex(1);
        request.setCourseId(99999L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> unitService.createUnit(request));
        assertTrue(ex.getMessage().toLowerCase().contains("course"));
    }

    @Test
    void getUnitById_existing_TC_US_001() {
        // TC-US-001: created unit can be queried by id.
        UnitRequest request = new UnitRequest();
        request.setTitle("Unit A");
        request.setOrderIndex(1);
        request.setCourseId(courseId);
        UnitResponse created = unitService.createUnit(request);

        UnitResponse found = unitService.getUnitById(created.getId());
        assertEquals("Unit A", found.getTitle());
    }

    @Test
    void updateUnit_existing_TC_US_008() {
        // TC-US-008: update should change title and order index.
        UnitRequest create = new UnitRequest();
        create.setTitle("Old Unit");
        create.setOrderIndex(1);
        create.setCourseId(courseId);
        UnitResponse created = unitService.createUnit(create);

        UnitRequest update = new UnitRequest();
        update.setTitle("Updated Unit");
        update.setOrderIndex(5);
        update.setCourseId(courseId);
        UnitResponse updated = unitService.updateUnit(created.getId(), update);

        // CheckDB: returned object reflects persisted values.
        assertEquals("Updated Unit", updated.getTitle());
        assertEquals(5, updated.getOrderIndex());
    }

    @Test
    void deleteUnit_thenNotFound_TC_US_011() {
        // TC-US-011: delete should remove unit row.
        UnitRequest request = new UnitRequest();
        request.setTitle("Delete Unit");
        request.setOrderIndex(1);
        request.setCourseId(courseId);
        UnitResponse created = unitService.createUnit(request);

        unitService.deleteUnit(created.getId());

        // CheckDB: deleted row must not exist.
        assertFalse(unitRepository.existsById(created.getId()));
        assertThrows(RuntimeException.class, () -> unitService.getUnitById(created.getId()));
    }
}
