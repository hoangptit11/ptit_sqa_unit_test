package ptit.com.enghub.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import ptit.com.enghub.dto.VocabularyDTO;
import ptit.com.enghub.dto.request.LessonCreationRequest;
import ptit.com.enghub.dto.response.LessonResponse;
import ptit.com.enghub.entity.Course;
import ptit.com.enghub.entity.Lesson;
import ptit.com.enghub.entity.Unit;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.UserProgress;
import ptit.com.enghub.entity.Vocabulary;
import ptit.com.enghub.enums.Level;
import ptit.com.enghub.enums.StudySkill;
import ptit.com.enghub.mapper.LessonMapper;
import ptit.com.enghub.repository.LessonRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.repository.UserProgressRepository;
import ptit.com.enghub.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VocabularyInLessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void given_AdminAndOneValidVocabulary_when_CreateLesson_then_SaveLessonWithOneVocabularyAndCorrectForeignKey() {
        // Test Case ID: VOCAB-TC-01
        // Test Objective: Tạo lesson với 1 vocabulary hợp lệ
        // Input: Authentication ROLE_ADMIN, LessonCreationRequest có vocabularies = [
        //          VocabularyDTO(word="run", meaning="chạy", example="I run every day.", imageUrl="http://img.com/run.jpg")
        //        ], unitRepository.findById hợp lệ
        // Expected Output: Lesson được save với vocabularies.size()==1,
        //                  vocab.getWord()=="run", vocab.getMeaning()=="chạy",
        //                  vocab.getLesson() == lesson (FK được set đúng)

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(List.of(buildVocabularyDto("run", "chạy", "I run every day.", "http://img.com/run.jpg")));

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson savedLesson = lessonService.createLesson(request);

        // Assert
        assertNotNull(savedLesson.getVocabularies());
        assertEquals(1, savedLesson.getVocabularies().size());
        Vocabulary savedVocabulary = savedLesson.getVocabularies().get(0);
        assertEquals("run", savedVocabulary.getWord());
        assertEquals("chạy", savedVocabulary.getMeaning());
        assertEquals(savedLesson, savedVocabulary.getLesson());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies()).hasSize(1);
        assertThat(capturedLesson.getVocabularies().get(0).getWord()).isEqualTo("run");
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_AdminAndFiveVocabularies_when_CreateLesson_then_SaveAllVocabularyItemsWithLessonReference() {
        // Test Case ID: VOCAB-TC-02
        // Test Objective: Tạo lesson với nhiều vocabulary
        // Input: Authentication ROLE_ADMIN, vocabularies có 5 phần tử
        // Expected Output: Lesson.getVocabularies().size() == 5, tất cả có lesson != null

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(List.of(
                buildVocabularyDto("w1", "m1", "e1", null),
                buildVocabularyDto("w2", "m2", "e2", null),
                buildVocabularyDto("w3", "m3", "e3", null),
                buildVocabularyDto("w4", "m4", "e4", null),
                buildVocabularyDto("w5", "m5", "e5", null)
        ));

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson savedLesson = lessonService.createLesson(request);

        // Assert
        assertEquals(5, savedLesson.getVocabularies().size());
        assertFalse(savedLesson.getVocabularies().stream().anyMatch(v -> v.getLesson() == null));
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies()).hasSize(5);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_AdminAndNullVocabularies_when_CreateLesson_then_LessonVocabularyListRemainsEmpty() {
        // Test Case ID: VOCAB-TC-03
        // Test Objective: Tạo lesson không có vocabulary (vocabularies = null)
        // Input: Authentication ROLE_ADMIN, LessonCreationRequest(vocabularies=null)
        // Expected Output: Lesson.getVocabularies() rỗng (không throw exception)

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(null);

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson savedLesson = assertDoesNotThrow(() -> lessonService.createLesson(request));

        // Assert
        assertNotNull(savedLesson.getVocabularies());
        assertEquals(0, savedLesson.getVocabularies().size());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies()).isEmpty();
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_AdminAndExistingLessonWithOldVocab_when_UpdateLesson_then_ReplaceOldVocabularyWithNewList() {
        // Test Case ID: VOCAB-TC-04
        // Test Objective: Update lesson — thay toàn bộ vocabulary cũ bằng vocabulary mới
        // Input: Authentication ROLE_ADMIN, id=1L,
        //        Lesson hiện tại có 3 vocab cũ,
        //        LessonCreationRequest mới có vocabularies = [
        //          VocabularyDTO(word="new_word", meaning="từ mới", example="...", imageUrl=null)
        //        ]
        // Expected Output: Sau khi save, lesson.getVocabularies().size()==1 (đã clear cũ, add mới)

        // Arrange
        setAdminAuthentication();
        Long lessonId = 1L;
        Unit existingUnit = buildUnit(1L);
        Lesson existingLesson = buildLesson(lessonId, existingUnit, "Old Lesson");
        existingLesson.getVocabularies().add(buildVocabularyEntity("old1", "m1", "e1", null, existingLesson));
        existingLesson.getVocabularies().add(buildVocabularyEntity("old2", "m2", "e2", null, existingLesson));
        existingLesson.getVocabularies().add(buildVocabularyEntity("old3", "m3", "e3", null, existingLesson));

        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setTitle("Updated Lesson");
        request.setVocabularies(List.of(buildVocabularyDto("new_word", "từ mới", "...", null)));

        LessonResponse expectedResponse = new LessonResponse();

        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(existingLesson)).willReturn(existingLesson);
        given(lessonMapper.toResponse(existingLesson)).willReturn(expectedResponse);

        // Act
        lessonService.updateLesson(lessonId, request);

        // Assert
        assertEquals(1, existingLesson.getVocabularies().size());
        assertEquals("new_word", existingLesson.getVocabularies().get(0).getWord());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson savedLessonInRepository = lessonCaptor.getValue();
        assertThat(savedLessonInRepository).isSameAs(existingLesson);
        assertThat(savedLessonInRepository.getVocabularies()).hasSize(1);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_AdminAndExistingVocab_when_UpdateLessonWithNullVocabularies_then_ClearAllVocabularyItems() {
        // Test Case ID: VOCAB-TC-05
        // Test Objective: Update lesson — xóa hết vocabulary (vocabularies = null)
        // Input: Authentication ROLE_ADMIN, id=1L,
        //        Lesson hiện tại có 2 vocab,
        //        LessonCreationRequest mới có vocabularies=null
        // Expected Output: lesson.getVocabularies() rỗng sau clear(), save được gọi

        // Arrange
        setAdminAuthentication();
        Long lessonId = 1L;
        Unit existingUnit = buildUnit(1L);
        Lesson existingLesson = buildLesson(lessonId, existingUnit, "Old Lesson");
        existingLesson.getVocabularies().add(buildVocabularyEntity("old1", "m1", "e1", null, existingLesson));
        existingLesson.getVocabularies().add(buildVocabularyEntity("old2", "m2", "e2", null, existingLesson));

        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(null);

        LessonResponse expectedResponse = new LessonResponse();
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(existingLesson)).willReturn(existingLesson);
        given(lessonMapper.toResponse(existingLesson)).willReturn(expectedResponse);

        // Act
        lessonService.updateLesson(lessonId, request);

        // Assert
        assertEquals(0, existingLesson.getVocabularies().size());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson savedLessonInRepository = lessonCaptor.getValue();
        assertThat(savedLessonInRepository).isSameAs(existingLesson);
        assertThat(savedLessonInRepository.getVocabularies()).isEmpty();
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_VocabularyImageUrlNull_when_CreateLesson_then_HandleNormallyWithoutNullPointer() {
        // Test Case ID: VOCAB-TC-06
        // Test Objective: Vocabulary có imageUrl = null vẫn được xử lý bình thường
        // Input: VocabularyDTO(word="cat", meaning="con mèo", example="The cat is cute.", imageUrl=null)
        // Expected Output: vocab.getImageUrl() == null, không có NullPointerException

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(List.of(buildVocabularyDto("cat", "con mèo", "The cat is cute.", null)));

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson savedLesson = assertDoesNotThrow(() -> lessonService.createLesson(request));

        // Assert
        Vocabulary savedVocabulary = savedLesson.getVocabularies().get(0);
        assertNull(savedVocabulary.getImageUrl());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies().get(0).getImageUrl()).isNull();
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_VocabularyWordNull_when_CreateLesson_then_ServiceStillSavesLessonAtServiceLayer() {
        // Test Case ID: VOCAB-TC-07
        // Test Objective: Vocabulary word bị null (edge case validation)
        // Input: VocabularyDTO(word=null, meaning="meaning", example="ex", imageUrl=null)
        // Expected Output: Lesson được save (validation không xảy ra ở service layer — DB sẽ báo lỗi)
        //                  Hoặc nếu có validation: throw ConstraintViolationException
        //                  [Ghi chú: Hiện tại code không validate ở service, entity có @Column(nullable=false)]

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = buildBaseCreateRequest(1L);
        request.setVocabularies(List.of(buildVocabularyDto(null, "meaning", "ex", null)));

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson savedLesson = assertDoesNotThrow(() -> lessonService.createLesson(request));

        // Assert
        assertEquals(1, savedLesson.getVocabularies().size());
        assertNull(savedLesson.getVocabularies().get(0).getWord());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies()).hasSize(1);
        assertThat(capturedLesson.getVocabularies().get(0).getWord()).isNull();
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    private void setAdminAuthentication() {
        Authentication auth = mock(Authentication.class);
        GrantedAuthority adminAuthority = () -> "ROLE_ADMIN";
        given(auth.getAuthorities()).willReturn((Collection) List.of(adminAuthority));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private LessonCreationRequest buildBaseCreateRequest(Long unitId) {
        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Lesson 1");
        request.setOrderIndex(1);
        request.setDuration(30);
        request.setUnitId(unitId);
        request.setStudySkill(StudySkill.VOCAB);
        return request;
    }

    private VocabularyDTO buildVocabularyDto(String word, String meaning, String example, String imageUrl) {
        VocabularyDTO vocabularyDTO = new VocabularyDTO();
        vocabularyDTO.setWord(word);
        vocabularyDTO.setMeaning(meaning);
        vocabularyDTO.setExample(example);
        vocabularyDTO.setImageUrl(imageUrl);
        return vocabularyDTO;
    }

    private Vocabulary buildVocabularyEntity(String word, String meaning, String example, String imageUrl, Lesson lesson) {
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setWord(word);
        vocabulary.setMeaning(meaning);
        vocabulary.setExample(example);
        vocabulary.setImageUrl(imageUrl);
        vocabulary.setLesson(lesson);
        return vocabulary;
    }

    private Lesson buildLesson(Long id, Unit unit, String title) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTitle(title);
        lesson.setOrderIndex(1);
        lesson.setDuration(30);
        lesson.setUnit(unit);
        lesson.setStudySkill(StudySkill.VOCAB);
        return lesson;
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setLevel(Level.BEGINNER);
        return user;
    }

    private Course buildCourse(Long id) {
        Course course = new Course();
        course.setId(id);
        course.setTitle("Course title");
        return course;
    }

    private Unit buildUnit(Long id) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setTitle("Unit title");
        unit.setOrderIndex(1);
        unit.setCourse(buildCourse(1L));
        return unit;
    }
}
