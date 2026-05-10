package ptit.com.enghub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ptit.com.enghub.dto.request.CourseRequest;
import ptit.com.enghub.dto.response.CourseResponse;
import ptit.com.enghub.entity.Course;
import ptit.com.enghub.entity.Language;
import ptit.com.enghub.mapper.CourseMapper;
import ptit.com.enghub.repository.CourseRepository;
import ptit.com.enghub.repository.LanguageRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void getAllCourses_shouldReturnAllCourses_whenDatabaseHasData() {
        // Test Case ID: COURSE-TC-01
        // Test Objective: Lấy danh sách tất cả course khi DB có dữ liệu
        // Input: courseRepository.findAll() trả về list 2 Course
        // Expected Output: List<CourseResponse> size = 2, courseMapper::toResponse được gọi 2 lần

        // Arrange
        Course firstCourse = Course.builder().id(1L).title("Course 1").build();
        Course secondCourse = Course.builder().id(2L).title("Course 2").build();
        CourseResponse firstResponse = new CourseResponse();
        CourseResponse secondResponse = new CourseResponse();

        given(courseRepository.findAll()).willReturn(Arrays.asList(firstCourse, secondCourse));
        given(courseMapper.toResponse(firstCourse)).willReturn(firstResponse);
        given(courseMapper.toResponse(secondCourse)).willReturn(secondResponse);

        // Act
        List<CourseResponse> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(courseMapper, times(1)).toResponse(firstCourse);
        verify(courseMapper, times(1)).toResponse(secondCourse);
    }

    @Test
    void getAllCourses_shouldReturnEmptyList_whenDatabaseIsEmpty() {
        // Test Case ID: COURSE-TC-02
        // Test Objective: Lấy danh sách course khi DB rỗng
        // Input: courseRepository.findAll() trả về empty list
        // Expected Output: List<CourseResponse> rỗng (size = 0)

        // Arrange
        given(courseRepository.findAll()).willReturn(Collections.emptyList());

        // Act
        List<CourseResponse> result = courseService.getAllCourses();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(courseMapper, never()).toResponse(any(Course.class));
    }

    @Test
    void getCourseById_shouldReturnCourseResponse_whenCourseExists() {
        // Test Case ID: COURSE-TC-03
        // Test Objective: Lấy course theo id hợp lệ tồn tại
        // Input: id = 1L, courseRepository.findById(1L) trả về Optional<Course>
        // Expected Output: CourseResponse không null, courseMapper.toResponse được gọi 1 lần

        // Arrange
        Long courseId = 1L;
        Course existingCourse = Course.builder().id(courseId).title("English Beginner").build();
        CourseResponse expectedResponse = new CourseResponse();
        expectedResponse.setId(courseId);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(existingCourse));
        given(courseMapper.toResponse(existingCourse)).willReturn(expectedResponse);

        // Act
        CourseResponse result = courseService.getCourseById(courseId);

        // Assert
        assertNotNull(result);
        assertEquals(courseId, result.getId());
        verify(courseMapper, times(1)).toResponse(existingCourse);
    }

    @Test
    void getCourseById_shouldThrowException_whenCourseDoesNotExist() {
        // Test Case ID: COURSE-TC-04
        // Test Objective: Lấy course theo id không tồn tại
        // Input: id = 999L, courseRepository.findById(999L) trả về Optional.empty()
        // Expected Output: Throw RuntimeException với message "Course not found"

        // Arrange
        Long courseId = 999L;
        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> courseService.getCourseById(courseId));

        // Assert
        assertEquals("Course not found", exception.getMessage());
        verify(courseMapper, never()).toResponse(any(Course.class));
    }

    @Test
    void createCourse_shouldCreateCourseSuccessfully_whenLanguageIdIsValid() {
        // Test Case ID: COURSE-TC-05
        // Test Objective: Tạo course mới với languageId hợp lệ
        // Input: CourseRequest(title="English Beginner", description="...", level=BEGINNER, languageId=1L)
        //        languageRepository.findById(1L) trả về Optional<Language>
        //        courseMapper.toEntity trả về Course mới
        //        courseRepository.save trả về Course đã lưu
        // Expected Output: CourseResponse không null, courseRepository.save() được gọi 1 lần

        // Arrange
        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setTitle("English Beginner");
        courseRequest.setDescription("...");
        courseRequest.setLevel("BEGINNER");
        courseRequest.setLanguageId(1L);

        Language existingLanguage = Language.builder().id(1L).name("English").code("en").build();
        Course mappedCourse = Course.builder().title("English Beginner").description("...").level("BEGINNER").build();
        Course savedCourse = Course.builder().id(1L).title("English Beginner").description("...").level("BEGINNER").language(existingLanguage).build();
        CourseResponse expectedResponse = new CourseResponse();
        expectedResponse.setId(1L);

        given(courseMapper.toEntity(courseRequest)).willReturn(mappedCourse);
        given(languageRepository.findById(1L)).willReturn(Optional.of(existingLanguage));
        given(courseRepository.save(mappedCourse)).willReturn(savedCourse);
        given(courseMapper.toResponse(savedCourse)).willReturn(expectedResponse);

        // Act
        CourseResponse result = courseService.createCourse(courseRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(existingLanguage, mappedCourse.getLanguage());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(courseCaptor.capture());
        Course savedCourseInRepository = courseCaptor.getValue();
        assertThat(savedCourseInRepository.getTitle()).isEqualTo("English Beginner");
        assertThat(savedCourseInRepository.getLanguage()).isEqualTo(existingLanguage);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void createCourse_shouldThrowException_whenLanguageIdDoesNotExist() {
        // Test Case ID: COURSE-TC-06
        // Test Objective: Tạo course với languageId không tồn tại
        // Input: CourseRequest(languageId=999L), languageRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException với message "Language not found"

        // Arrange
        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setLanguageId(999L);
        Course mappedCourse = Course.builder().title("Any course").build();

        given(courseMapper.toEntity(courseRequest)).willReturn(mappedCourse);
        given(languageRepository.findById(999L)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> courseService.createCourse(courseRequest));

        // Assert
        assertEquals("Language not found", exception.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_shouldUpdateCourseAndLanguage_whenCourseExistsAndNewLanguageProvided() {
        // Test Case ID: COURSE-TC-07
        // Test Objective: Cập nhật course khi id tồn tại, có languageId mới
        // Input: id=1L, CourseRequest(title="Updated", languageId=2L)
        //        courseRepository.findById(1L) = Optional<Course>
        //        languageRepository.findById(2L) = Optional<Language>
        // Expected Output: courseMapper.updateCourseFromRequest được gọi, language mới được set, save gọi 1 lần

        // Arrange
        Long courseId = 1L;
        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setTitle("Updated");
        courseRequest.setLanguageId(2L);

        Language oldLanguage = Language.builder().id(1L).name("English").code("en").build();
        Language newLanguage = Language.builder().id(2L).name("French").code("fr").build();
        Course existingCourse = Course.builder().id(courseId).title("Old title").language(oldLanguage).build();
        Course savedCourse = Course.builder().id(courseId).title("Updated").language(newLanguage).build();
        CourseResponse expectedResponse = new CourseResponse();
        expectedResponse.setId(courseId);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(existingCourse));
        given(languageRepository.findById(2L)).willReturn(Optional.of(newLanguage));
        given(courseRepository.save(existingCourse)).willReturn(savedCourse);
        given(courseMapper.toResponse(savedCourse)).willReturn(expectedResponse);

        // Act
        CourseResponse result = courseService.updateCourse(courseId, courseRequest);

        // Assert
        assertNotNull(result);
        assertEquals(courseId, result.getId());
        assertEquals(newLanguage, existingCourse.getLanguage());
        verify(courseMapper, times(1)).updateCourseFromRequest(courseRequest, existingCourse);
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(courseCaptor.capture());
        Course savedCourseInRepository = courseCaptor.getValue();
        assertThat(savedCourseInRepository.getId()).isEqualTo(courseId);
        assertThat(savedCourseInRepository.getLanguage()).isEqualTo(newLanguage);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void updateCourse_shouldThrowException_whenCourseDoesNotExist() {
        // Test Case ID: COURSE-TC-08
        // Test Objective: Cập nhật course khi id không tồn tại
        // Input: id=999L, courseRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException("Course not found")

        // Arrange
        Long courseId = 999L;
        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setTitle("Updated");
        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> courseService.updateCourse(courseId, courseRequest));

        // Assert
        assertEquals("Course not found", exception.getMessage());
        verify(courseMapper, never()).updateCourseFromRequest(any(CourseRequest.class), any(Course.class));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateCourse_shouldNotChangeLanguage_whenRequestLanguageIdIsNull() {
        // Test Case ID: COURSE-TC-09
        // Test Objective: Cập nhật course khi request.languageId = null (không đổi language)
        // Input: id=1L, CourseRequest(title="Updated", languageId=null)
        // Expected Output: languageRepository.findById KHÔNG được gọi, save vẫn được gọi

        // Arrange
        Long courseId = 1L;
        Language existingLanguage = Language.builder().id(1L).name("English").code("en").build();
        Course existingCourse = Course.builder().id(courseId).title("Old title").language(existingLanguage).build();
        Course savedCourse = Course.builder().id(courseId).title("Updated").language(existingLanguage).build();
        CourseResponse expectedResponse = new CourseResponse();
        expectedResponse.setId(courseId);

        CourseRequest courseRequest = new CourseRequest();
        courseRequest.setTitle("Updated");
        courseRequest.setLanguageId(null);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(existingCourse));
        given(courseRepository.save(existingCourse)).willReturn(savedCourse);
        given(courseMapper.toResponse(savedCourse)).willReturn(expectedResponse);

        // Act
        CourseResponse result = courseService.updateCourse(courseId, courseRequest);

        // Assert
        assertNotNull(result);
        assertEquals(courseId, result.getId());
        assertEquals(existingLanguage, existingCourse.getLanguage());
        verify(languageRepository, never()).findById(any(Long.class));
        verify(courseMapper, times(1)).updateCourseFromRequest(courseRequest, existingCourse);
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(courseCaptor.capture());
        Course savedCourseInRepository = courseCaptor.getValue();
        assertThat(savedCourseInRepository.getId()).isEqualTo(courseId);
        assertThat(savedCourseInRepository.getLanguage()).isEqualTo(existingLanguage);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void deleteCourse_shouldDeleteCourse_whenCourseExists() {
        // Test Case ID: COURSE-TC-10
        // Test Objective: Xóa course khi id tồn tại
        // Input: id=1L, courseRepository.existsById(1L) = true
        // Expected Output: courseRepository.deleteById(1L) được gọi đúng 1 lần

        // Arrange
        Long courseId = 1L;
        given(courseRepository.existsById(courseId)).willReturn(true);

        // Act
        courseService.deleteCourse(courseId);

        // Assert
        verify(courseRepository, times(1)).existsById(courseId);
        // CheckDB: Xac minh deleteById() duoc goi voi dung tham so
        ArgumentCaptor<Long> courseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(courseRepository, times(1)).deleteById(courseIdCaptor.capture());
        assertThat(courseIdCaptor.getValue()).isEqualTo(courseId);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void deleteCourse_shouldThrowException_whenCourseDoesNotExist() {
        // Test Case ID: COURSE-TC-11
        // Test Objective: Xóa course khi id không tồn tại
        // Input: id=999L, courseRepository.existsById(999L) = false
        // Expected Output: Throw RuntimeException("Course not found"), deleteById KHÔNG được gọi

        // Arrange
        Long courseId = 999L;
        given(courseRepository.existsById(courseId)).willReturn(false);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> courseService.deleteCourse(courseId));

        // Assert
        assertEquals("Course not found", exception.getMessage());
        verify(courseRepository, times(1)).existsById(courseId);
        verify(courseRepository, never()).deleteById(courseId);
    }
}
