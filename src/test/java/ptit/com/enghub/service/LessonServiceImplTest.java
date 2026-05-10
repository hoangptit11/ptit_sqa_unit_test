package ptit.com.enghub.service;

import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import ptit.com.enghub.dto.VocabularyDTO;
import ptit.com.enghub.dto.request.CompleteLessonRequest;
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
import ptit.com.enghub.exception.AppException;
import ptit.com.enghub.exception.ErrorCode;
import ptit.com.enghub.mapper.LessonMapper;
import ptit.com.enghub.repository.LessonRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.repository.UserProgressRepository;
import ptit.com.enghub.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

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
    void given_ValidLessonIdAndUncompletedProgress_when_GetLesson_then_ReturnResponseWithCompletedFalse() {
        // Test Case ID: LESSON-TC-01
        // Test Objective: Lấy lesson theo id hợp lệ, lesson chưa hoàn thành
        // Input: lessonId=1L, user tồn tại, lesson tồn tại, userProgress.isCompleted()=false
        // Expected Output: LessonResponse với completed=false

        // Arrange
        Long lessonId = 1L;
        User existingUser = buildUser(10L);
        Lesson existingLesson = buildLesson(lessonId, buildUnit(1L), 0, "Lesson 1");
        LessonResponse expectedResponse = new LessonResponse();
        UserProgress existingProgress = new UserProgress();
        existingProgress.setCompleted(false);

        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(lessonMapper.toResponse(existingLesson)).willReturn(expectedResponse);
        given(userProgressRepository.findByUserIdAndLessonId(existingUser.getId(), lessonId))
                .willReturn(Optional.of(existingProgress));

        // Act
        LessonResponse result = lessonService.getLesson(lessonId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isCompleted());
        verify(lessonMapper, times(1)).toResponse(existingLesson);
    }

    @Test
    void given_ValidLessonIdAndCompletedProgress_when_GetLesson_then_ReturnResponseWithCompletedTrue() {
        // Test Case ID: LESSON-TC-02
        // Test Objective: Lấy lesson theo id hợp lệ, lesson đã hoàn thành
        // Input: lessonId=1L, user tồn tại, lesson tồn tại, userProgress.isCompleted()=true
        // Expected Output: LessonResponse với completed=true

        // Arrange
        Long lessonId = 1L;
        User existingUser = buildUser(10L);
        Lesson existingLesson = buildLesson(lessonId, buildUnit(1L), 0, "Lesson 1");
        LessonResponse expectedResponse = new LessonResponse();
        UserProgress existingProgress = new UserProgress();
        existingProgress.setCompleted(true);

        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(lessonMapper.toResponse(existingLesson)).willReturn(expectedResponse);
        given(userProgressRepository.findByUserIdAndLessonId(existingUser.getId(), lessonId))
                .willReturn(Optional.of(existingProgress));

        // Act
        LessonResponse result = lessonService.getLesson(lessonId);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.isCompleted());
        verify(lessonMapper, times(1)).toResponse(existingLesson);
    }

    @Test
    void given_InvalidLessonId_when_GetLesson_then_ThrowRuntimeException() {
        // Test Case ID: LESSON-TC-03
        // Test Objective: Lấy lesson khi lessonId không tồn tại
        // Input: lessonId=999L, lessonRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException("Lesson not found")

        // Arrange
        Long lessonId = 999L;
        User existingUser = buildUser(10L);
        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> lessonService.getLesson(lessonId));

        // Assert
        assertEquals("Lesson not found", exception.getMessage());
        verify(lessonMapper, never()).toResponse(any(Lesson.class));
    }

    @Test
    void given_ValidLessonIdAndNoUserProgress_when_GetLesson_then_ReturnResponseWithDefaultCompletedFalse() {
        // Test Case ID: LESSON-TC-04
        // Test Objective: Lấy lesson khi user chưa có UserProgress nào (lần đầu học)
        // Input: lessonId=1L, userProgressRepository.findByUserIdAndLessonId(...) = Optional.empty()
        // Expected Output: LessonResponse với completed=false (default)

        // Arrange
        Long lessonId = 1L;
        User existingUser = buildUser(10L);
        Lesson existingLesson = buildLesson(lessonId, buildUnit(1L), 0, "Lesson 1");
        LessonResponse expectedResponse = new LessonResponse();

        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(lessonMapper.toResponse(existingLesson)).willReturn(expectedResponse);
        given(userProgressRepository.findByUserIdAndLessonId(existingUser.getId(), lessonId))
                .willReturn(Optional.empty());

        // Act
        LessonResponse result = lessonService.getLesson(lessonId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isCompleted());
    }

    @Test
    void given_ValidUnitIdWithLessons_when_GetLessonsByUnitId_then_ReturnLessonResponseList() {
        // Test Case ID: LESSON-TC-05
        // Test Objective: Lấy danh sách lesson theo unitId hợp lệ
        // Input: unitId=1L, DB có 3 lesson thuộc unit này
        // Expected Output: List<LessonResponse> size=3

        // Arrange
        Long unitId = 1L;
        Unit existingUnit = buildUnit(unitId);
        Lesson lesson1 = buildLesson(1L, existingUnit, 0, "Lesson 1");
        Lesson lesson2 = buildLesson(2L, existingUnit, 1, "Lesson 2");
        Lesson lesson3 = buildLesson(3L, existingUnit, 2, "Lesson 3");
        LessonResponse response1 = new LessonResponse();
        LessonResponse response2 = new LessonResponse();
        LessonResponse response3 = new LessonResponse();

        given(lessonRepository.findByUnit_Id(unitId)).willReturn(List.of(lesson1, lesson2, lesson3));
        given(lessonMapper.toResponse(lesson1)).willReturn(response1);
        given(lessonMapper.toResponse(lesson2)).willReturn(response2);
        given(lessonMapper.toResponse(lesson3)).willReturn(response3);

        // Act
        List<LessonResponse> result = lessonService.getLessonsByUnitId(unitId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(lessonMapper, times(3)).toResponse(any(Lesson.class));
    }

    @Test
    void given_UnitWithoutLessons_when_GetLessonsByUnitId_then_ReturnEmptyList() {
        // Test Case ID: LESSON-TC-06
        // Test Objective: Lấy danh sách lesson khi unit không có lesson nào
        // Input: unitId=99L, lessonRepository.findByUnit_Id(99L) = empty list
        // Expected Output: List<LessonResponse> rỗng

        // Arrange
        Long unitId = 99L;
        given(lessonRepository.findByUnit_Id(unitId)).willReturn(Collections.emptyList());

        // Act
        List<LessonResponse> result = lessonService.getLessonsByUnitId(unitId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(lessonMapper, never()).toResponse(any(Lesson.class));
    }

    @Test
    void given_NoExistingUserProgress_when_CompleteLesson_then_CreateAndSaveNewProgress() {
        // Test Case ID: LESSON-TC-07
        // Test Objective: Hoàn thành lesson lần đầu (chưa có UserProgress)
        // Input: lessonId=1L, CompleteLessonRequest(score=90),
        //        userProgressRepository.findByUserIdAndLessonId() = Optional.empty()
        // Expected Output: UserProgress mới được tạo và save với completed=true, score=90
        //                  notificationService.create() được gọi 1 lần

        // Arrange
        Long lessonId = 1L;
        User existingUser = buildUser(10L);
        Unit existingUnit = buildUnit(1L);
        Lesson existingLesson = buildLesson(lessonId, existingUnit, 0, "Lesson 1");
        CompleteLessonRequest request = new CompleteLessonRequest();
        request.setScore(90);

        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(userProgressRepository.findByUserIdAndLessonId(existingUser.getId(), lessonId)).willReturn(Optional.empty());
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 21L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 22L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 23L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 24L)).willReturn(false);
        given(lessonRepository.findByUnit_Id(existingUnit.getId())).willReturn(Collections.emptyList());

        // Act
        lessonService.completeLesson(lessonId, request);

        // Assert
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository, times(1)).save(progressCaptor.capture());
        UserProgress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress.isCompleted()).isTrue();
        assertThat(savedProgress.getScore()).isEqualTo(90);
        assertThat(savedProgress.getUserId()).isEqualTo(existingUser.getId());
        verify(notificationService, times(1)).create(any());
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_ExistingUserProgress_when_CompleteLesson_then_UpdateAndSaveProgress() {
        // Test Case ID: LESSON-TC-08
        // Test Objective: Hoàn thành lesson lần thứ 2 (đã có UserProgress trước đó)
        // Input: lessonId=1L, CompleteLessonRequest(score=80),
        //        userProgressRepository.findByUserIdAndLessonId() = Optional<UserProgress(completed=false)>
        // Expected Output: UserProgress cũ được update completed=true, score=80, save gọi 1 lần

        // Arrange
        Long lessonId = 1L;
        User existingUser = buildUser(10L);
        Unit existingUnit = buildUnit(1L);
        Lesson existingLesson = buildLesson(lessonId, existingUnit, 0, "Lesson 1");
        CompleteLessonRequest request = new CompleteLessonRequest();
        request.setScore(80);
        UserProgress existingProgress = new UserProgress();
        existingProgress.setCompleted(false);
        existingProgress.setScore(10);

        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(userProgressRepository.findByUserIdAndLessonId(existingUser.getId(), lessonId))
                .willReturn(Optional.of(existingProgress));
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 21L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 22L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 23L)).willReturn(false);
        given(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(existingUser.getId(), 24L)).willReturn(false);
        given(lessonRepository.findByUnit_Id(existingUnit.getId())).willReturn(Collections.emptyList());

        // Act
        lessonService.completeLesson(lessonId, request);

        // Assert
        assertEquals(true, existingProgress.isCompleted());
        assertEquals(80, existingProgress.getScore());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<UserProgress> progressCaptor = ArgumentCaptor.forClass(UserProgress.class);
        verify(userProgressRepository, times(1)).save(progressCaptor.capture());
        UserProgress savedProgress = progressCaptor.getValue();
        assertThat(savedProgress).isSameAs(existingProgress);
        assertThat(savedProgress.isCompleted()).isTrue();
        assertThat(savedProgress.getScore()).isEqualTo(80);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_InvalidLessonId_when_CompleteLesson_then_ThrowRuntimeException() {
        // Test Case ID: LESSON-TC-09
        // Test Objective: Hoàn thành lesson khi lesson không tồn tại
        // Input: lessonId=999L, lessonRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException("Lesson not found")

        // Arrange
        Long lessonId = 999L;
        User existingUser = buildUser(10L);
        CompleteLessonRequest request = new CompleteLessonRequest();
        request.setScore(50);
        given(userService.getCurrentUser()).willReturn(existingUser);
        given(lessonRepository.findById(lessonId)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> lessonService.completeLesson(lessonId, request));

        // Assert
        assertEquals("Lesson not found", exception.getMessage());
        verify(userProgressRepository, never()).save(any(UserProgress.class));
    }

    @Test
    void given_AdminRoleAndMinimalRequest_when_CreateLesson_then_SaveAndReturnLesson() {
        // Test Case ID: LESSON-TC-10
        // Test Objective: Admin tạo lesson mới thành công (chỉ có title, orderIndex, duration — không có content phụ)
        // Input: Authentication có ROLE_ADMIN,
        //        LessonCreationRequest(title="Lesson 1", orderIndex=0, duration=30, unitId=1L,
        //          studySkill=VOCABULARY, video=null, vocabularies=null, dialogues=null, grammar=null, exercises=null)
        //        unitRepository.findById(1L) = Optional<Unit>
        // Expected Output: lessonRepository.save() được gọi, trả về Lesson đã lưu

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Lesson 1");
        request.setOrderIndex(0);
        request.setDuration(30);
        request.setUnitId(1L);
        request.setStudySkill(StudySkill.VOCAB);

        Lesson savedLesson = buildLesson(100L, existingUnit, 0, "Lesson 1");
        savedLesson.setDuration(30);
        savedLesson.setStudySkill(StudySkill.VOCAB);

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willReturn(savedLesson);

        // Act
        Lesson result = lessonService.createLesson(request);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getTitle()).isEqualTo("Lesson 1");
        assertThat(capturedLesson.getUnit()).isEqualTo(existingUnit);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_NonAdminRole_when_CreateLesson_then_ThrowUnauthorizedException() {
        // Test Case ID: LESSON-TC-11
        // Test Objective: Non-admin cố tạo lesson → bị từ chối
        // Input: Authentication KHÔNG có ROLE_ADMIN (chỉ có ROLE_USER)
        // Expected Output: Throw AppException với ErrorCode.UNAUTHORIZED

        // Arrange
        setUserAuthentication();
        LessonCreationRequest request = new LessonCreationRequest();

        // Act
        AppException exception = assertThrows(AppException.class, () -> lessonService.createLesson(request));

        // Assert
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void given_AdminRoleAndMissingUnit_when_CreateLesson_then_ThrowRuntimeException() {
        // Test Case ID: LESSON-TC-12
        // Test Objective: Admin tạo lesson với unitId không tồn tại
        // Input: Authentication có ROLE_ADMIN, LessonCreationRequest(unitId=999L),
        //        unitRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException("Unit not found")

        // Arrange
        setAdminAuthentication();
        LessonCreationRequest request = new LessonCreationRequest();
        request.setUnitId(999L);

        given(unitRepository.findById(999L)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> lessonService.createLesson(request));

        // Assert
        assertEquals("Unit not found", exception.getMessage());
        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void given_AdminRoleAndVocabularyList_when_CreateLesson_then_SaveLessonWithVocabularies() {
        // Test Case ID: LESSON-TC-13
        // Test Objective: Admin tạo lesson kèm vocabulary list
        // Input: Authentication ROLE_ADMIN, LessonCreationRequest có vocabularies = [
        //          VocabularyDTO(word="apple", meaning="quả táo", example="I eat an apple", imageUrl=null),
        //          VocabularyDTO(word="book", meaning="quyển sách", example="I read a book", imageUrl=null)
        //        ], unitRepository.findById trả về Unit hợp lệ
        // Expected Output: Lesson được save với 2 Vocabulary trong lesson.getVocabularies()

        // Arrange
        setAdminAuthentication();
        Unit existingUnit = buildUnit(1L);
        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Vocabulary Lesson");
        request.setOrderIndex(1);
        request.setDuration(20);
        request.setUnitId(1L);
        request.setStudySkill(StudySkill.VOCAB);

        VocabularyDTO firstVocabulary = new VocabularyDTO();
        firstVocabulary.setWord("apple");
        firstVocabulary.setMeaning("quả táo");
        firstVocabulary.setExample("I eat an apple");

        VocabularyDTO secondVocabulary = new VocabularyDTO();
        secondVocabulary.setWord("book");
        secondVocabulary.setMeaning("quyển sách");
        secondVocabulary.setExample("I read a book");

        request.setVocabularies(List.of(firstVocabulary, secondVocabulary));

        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(any(Lesson.class))).willAnswer(invocation -> invocation.getArgument(0));

        // Act
        Lesson result = lessonService.createLesson(request);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getVocabularies());
        assertEquals(2, result.getVocabularies().size());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson capturedLesson = lessonCaptor.getValue();
        assertThat(capturedLesson.getVocabularies()).hasSize(2);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_AdminRoleAndExistingLesson_when_DeleteLesson_then_DeleteSuccessfully() {
        // Test Case ID: LESSON-TC-14
        // Test Objective: Admin xóa lesson tồn tại
        // Input: Authentication ROLE_ADMIN, id=1L, lessonRepository.findById(1L) = Optional<Lesson>
        // Expected Output: lessonRepository.delete(lesson) được gọi 1 lần

        // Arrange
        setAdminAuthentication();
        Long lessonId = 1L;
        Lesson existingLesson = buildLesson(lessonId, buildUnit(1L), 0, "Lesson 1");

        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));

        // Act
        lessonService.deleteLesson(lessonId);

        // Assert
        // CheckDB: Xac minh delete() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).delete(lessonCaptor.capture());
        assertThat(lessonCaptor.getValue()).isSameAs(existingLesson);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_NonAdminRole_when_DeleteLesson_then_ThrowUnauthorizedException() {
        // Test Case ID: LESSON-TC-15
        // Test Objective: Non-admin cố xóa lesson
        // Input: Authentication ROLE_USER, id=1L
        // Expected Output: Throw AppException(ErrorCode.UNAUTHORIZED)

        // Arrange
        setUserAuthentication();
        Long lessonId = 1L;

        // Act
        AppException exception = assertThrows(AppException.class, () -> lessonService.deleteLesson(lessonId));

        // Assert
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(lessonRepository, never()).delete(any(Lesson.class));
    }

    @Test
    void given_AdminRoleAndMissingLesson_when_DeleteLesson_then_ThrowRuntimeException() {
        // Test Case ID: LESSON-TC-16
        // Test Objective: Admin xóa lesson không tồn tại
        // Input: Authentication ROLE_ADMIN, id=999L, lessonRepository.findById(999L) = Optional.empty()
        // Expected Output: Throw RuntimeException("Lesson not found")

        // Arrange
        setAdminAuthentication();
        Long lessonId = 999L;
        given(lessonRepository.findById(lessonId)).willReturn(Optional.empty());

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> lessonService.deleteLesson(lessonId));

        // Assert
        assertEquals("Lesson not found", exception.getMessage());
        verify(lessonRepository, never()).delete(any(Lesson.class));
    }

    @Test
    void given_AdminRoleAndValidRequest_when_UpdateLesson_then_SaveAndReturnResponse() {
        // Test Case ID: LESSON-TC-17
        // Test Objective: Admin cập nhật lesson thành công
        // Input: Authentication ROLE_ADMIN, id=1L,
        //        LessonCreationRequest(title="Updated Title", orderIndex=1, duration=45, unitId=1L)
        //        lessonRepository.findById(1L) = Optional<Lesson>
        //        unitRepository.findById(1L) = Optional<Unit>
        // Expected Output: lessonRepository.save() gọi 1 lần, lessonMapper.toResponse() gọi 1 lần

        // Arrange
        setAdminAuthentication();
        Long lessonId = 1L;
        Unit existingUnit = buildUnit(1L);
        Lesson existingLesson = buildLesson(lessonId, existingUnit, 0, "Old Title");

        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Updated Title");
        request.setOrderIndex(1);
        request.setDuration(45);
        request.setUnitId(1L);
        request.setStudySkill(StudySkill.VOCAB);

        Lesson savedLesson = buildLesson(lessonId, existingUnit, 1, "Updated Title");
        savedLesson.setDuration(45);
        savedLesson.setStudySkill(StudySkill.VOCAB);

        LessonResponse expectedResponse = new LessonResponse();
        expectedResponse.setId(lessonId);

        given(lessonRepository.findById(lessonId)).willReturn(Optional.of(existingLesson));
        given(unitRepository.findById(1L)).willReturn(Optional.of(existingUnit));
        given(lessonRepository.save(existingLesson)).willReturn(savedLesson);
        given(lessonMapper.toResponse(savedLesson)).willReturn(expectedResponse);

        // Act
        LessonResponse result = lessonService.updateLesson(lessonId, request);

        // Assert
        assertNotNull(result);
        assertEquals(lessonId, result.getId());
        // CheckDB: Xac minh save() duoc goi voi dung object
        ArgumentCaptor<Lesson> lessonCaptor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository, times(1)).save(lessonCaptor.capture());
        Lesson savedLessonInRepository = lessonCaptor.getValue();
        assertThat(savedLessonInRepository).isSameAs(existingLesson);
        assertThat(savedLessonInRepository.getTitle()).isEqualTo("Updated Title");
        verify(lessonMapper, times(1)).toResponse(savedLesson);
        // Rollback: Day la unit test voi mock - khong co DB that.
        // Sau khi test ket thuc, moi thay doi chi ton tai trong mock, tu dong bi huy.
        // Trong integration test that, dung @Transactional tren test method de auto-rollback.
    }

    @Test
    void given_NonAdminRole_when_UpdateLesson_then_ThrowUnauthorizedException() {
        // Test Case ID: LESSON-TC-18
        // Test Objective: Non-admin cố cập nhật lesson
        // Input: Authentication ROLE_USER
        // Expected Output: Throw AppException(ErrorCode.UNAUTHORIZED)

        // Arrange
        setUserAuthentication();
        Long lessonId = 1L;
        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Updated Title");

        // Act
        AppException exception = assertThrows(AppException.class, () -> lessonService.updateLesson(lessonId, request));

        // Assert
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(lessonMapper, never()).toResponse(any(Lesson.class));
    }

    @Test
    void completeLesson_shouldCallGetCurrentUserOnlyOnce() {
        User user = buildUserWithLevel(1L, Level.BEGINNER);
        Lesson lesson = buildLesson(10L, buildUnit(1L), 1, "Lesson 10");
        CompleteLessonRequest request = new CompleteLessonRequest();
        request.setScore(80);

        when(userService.getCurrentUser()).thenReturn(user);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(userProgressRepository.findByUserIdAndLessonId(1L, 10L)).thenReturn(Optional.empty());
        when(lessonRepository.findByUnit_Id(1L)).thenReturn(List.of(lesson));
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(any(Long.class), any(Long.class))).thenReturn(false);

        lessonService.completeLesson(10L, request);

        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    void updateUserLevel_beginnerCompletes23And24_shouldNotSkipIntermediate() {
        User user = buildUserWithLevel(1L, Level.BEGINNER);
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(1L, 21L)).thenReturn(false);
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(1L, 22L)).thenReturn(false);
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(1L, 23L)).thenReturn(true);
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(1L, 24L)).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);

        lessonService.updateUserLevel(1L);

        assertEquals(Level.BEGINNER, user.getLevel());
    }

    @Test
    void updateUserLevel_advancedUser_shouldBeAbleToReachProficiency() {
        User user = buildUserWithLevel(1L, Level.ADVANCED);
        when(userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(any(Long.class), any(Long.class))).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);

        lessonService.updateUserLevel(1L);

        assertEquals(Level.PROFICIENCY, user.getLevel());
    }

    @Test
    void isLessonUnlocked_firstLessonWithOrderIndex1_returnsTrue() {
        User user = buildUserWithLevel(1L, Level.BEGINNER);
        Unit unit = buildUnit(1L);
        Lesson firstLesson = buildLesson(100L, unit, 1, "Lesson 1");
        when(userService.getCurrentUser()).thenReturn(user);
        when(lessonRepository.findByUnit_Id(1L)).thenReturn(List.of(firstLesson));

        boolean result = invokeIsLessonUnlocked(firstLesson);

        assertTrue(result);
    }

    @Test
    void lessonEntity_vocabularies_shouldHaveOrphanRemoval() throws Exception {
        Field field = Lesson.class.getDeclaredField("vocabularies");
        OneToMany annotation = field.getAnnotation(OneToMany.class);
        assertNotNull(annotation);
        assertTrue(annotation.orphanRemoval());
    }

    @Test
    void lessonEntity_exercises_shouldHaveOrphanRemoval() throws Exception {
        Field field = Lesson.class.getDeclaredField("exercises");
        OneToMany annotation = field.getAnnotation(OneToMany.class);
        assertNotNull(annotation);
        assertTrue(annotation.orphanRemoval());
    }

    @Test
    void updateLesson_clearVocabularies_javaListEmptied_butNoDeleteCalledOnDb() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        Unit unit = buildUnit(1L);
        Lesson lesson = buildLesson(1L, unit, 1, "Lesson 1");
        Vocabulary oldVocab = new Vocabulary();
        oldVocab.setId(999L);
        oldVocab.setWord("old");
        oldVocab.setMeaning("old meaning");
        lesson.getVocabularies().add(oldVocab);

        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Updated");
        request.setOrderIndex(1);
        request.setStudySkill(StudySkill.VOCAB);
        request.setVocabularies(List.of());

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        lessonService.updateLesson(1L, request);

        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonRepository).save(captor.capture());
        assertTrue(captor.getValue().getVocabularies().isEmpty());
    }

    private void setAdminAuthentication() {
        Authentication auth = mock(Authentication.class);
        GrantedAuthority adminAuthority = () -> "ROLE_ADMIN";
        given(auth.getAuthorities()).willReturn((Collection) List.of(adminAuthority));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private void setUserAuthentication() {
        Authentication auth = mock(Authentication.class);
        GrantedAuthority userAuthority = () -> "ROLE_USER";
        given(auth.getAuthorities()).willReturn((Collection) List.of(userAuthority));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private Lesson buildLesson(Long id, Unit unit, int orderIndex, String title) {
        Lesson lesson = new Lesson();
        lesson.setId(id);
        lesson.setTitle(title);
        lesson.setOrderIndex(orderIndex);
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

    private User buildUserWithLevel(Long id, Level level) {
        User user = new User();
        user.setId(id);
        user.setLevel(level);
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

    private boolean invokeIsLessonUnlocked(Lesson lesson) {
        try {
            Method method = LessonServiceImpl.class.getDeclaredMethod("isLessonUnlocked", Lesson.class);
            method.setAccessible(true);
            return (boolean) method.invoke(lessonService, lesson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
