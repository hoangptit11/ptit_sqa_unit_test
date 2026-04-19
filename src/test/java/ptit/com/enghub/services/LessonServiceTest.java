package ptit.com.enghub.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ptit.com.enghub.dto.request.LessonCreationRequest;
import ptit.com.enghub.dto.response.LessonResponse;
import ptit.com.enghub.entity.Lesson;
import ptit.com.enghub.entity.Unit;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.UserProgress;
import ptit.com.enghub.exception.AppException;
import ptit.com.enghub.mapper.LessonMapper;
import ptit.com.enghub.repository.LessonRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.repository.UserProgressRepository;
import ptit.com.enghub.repository.UserRepository;
import ptit.com.enghub.service.LessonServiceImpl;
import ptit.com.enghub.service.NotificationService;
import ptit.com.enghub.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

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
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getLesson_existing_TC_LS_003() {
        // TC-LS-003: existing lesson returns full response + completed flag.
        Lesson lesson = new Lesson();
        lesson.setId(10L);
        lesson.setTitle("Lesson 1");

        LessonResponse mapped = new LessonResponse();
        mapped.setId(10L);
        mapped.setTitle("Lesson 1");

        User currentUser = new User();
        currentUser.setId(7L);
        UserProgress progress = new UserProgress();
        progress.setCompleted(true);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(lessonMapper.toResponse(lesson)).thenReturn(mapped);
        when(userProgressRepository.findByUserIdAndLessonId(7L, 10L)).thenReturn(Optional.of(progress));

        LessonResponse response = lessonService.getLesson(10L);

        assertEquals("Lesson 1", response.getTitle());
        assertTrue(response.isCompleted());
    }

    @Test
    void getLesson_notFound_TC_LS_004() {
        // TC-LS-004: non-existing lesson id should throw.
        User currentUser = new User();
        currentUser.setId(7L);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(lessonRepository.findById(404L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> lessonService.getLesson(404L));
        assertTrue(ex.getMessage().contains("Lesson not found"));
    }

    @Test
    void getLessonsByUnitId_TC_LS_001() {
        // TC-LS-001: list lessons by valid unit id.
        Lesson lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("By Unit");
        LessonResponse mapped = new LessonResponse();
        mapped.setId(1L);
        mapped.setTitle("By Unit");

        when(lessonRepository.findByUnit_Id(33L)).thenReturn(List.of(lesson));
        when(lessonMapper.toResponse(lesson)).thenReturn(mapped);

        List<LessonResponse> responses = lessonService.getLessonsByUnitId(33L);
        assertEquals(1, responses.size());
        assertEquals("By Unit", responses.get(0).getTitle());
    }

    @Test
    void createLesson_unauthorized_TC_AUTH_001() {
        // TC-AUTH-001: non-admin user cannot create lesson.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        LessonCreationRequest request = new LessonCreationRequest();
        request.setUnitId(1L);
        request.setTitle("New lesson");
        request.setOrderIndex(1);
        request.setDuration(5);

        assertThrows(AppException.class, () -> lessonService.createLesson(request));
    }

    @Test
    void createLesson_unitNotFound_TC_LS_009() {
        // TC-LS-009: admin create with invalid unit id should fail.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        LessonCreationRequest request = new LessonCreationRequest();
        request.setUnitId(999L);
        request.setTitle("New lesson");
        request.setOrderIndex(1);
        request.setDuration(5);

        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> lessonService.createLesson(request));
        assertTrue(ex.getMessage().contains("Unit not found"));
    }

    @Test
    void createLesson_adminSuccess_TC_LS_005() {
        // TC-LS-005: admin with valid data can create lesson successfully.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        Unit unit = new Unit();
        unit.setId(21L);

        LessonCreationRequest request = new LessonCreationRequest();
        request.setUnitId(21L);
        request.setTitle("Lesson admin");
        request.setOrderIndex(1);
        request.setDuration(10);

        when(unitRepository.findById(21L)).thenReturn(Optional.of(unit));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson saved = invocation.getArgument(0);
            saved.setId(1000L);
            return saved;
        });

        Lesson saved = lessonService.createLesson(request);
        assertNotNull(saved.getId());
        assertEquals("Lesson admin", saved.getTitle());
    }

    @Test
    void updateLesson_unauthorized_TC_LS_020() {
        // TC-LS-020: non-admin user cannot update lesson.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        LessonCreationRequest request = new LessonCreationRequest();
        request.setTitle("Denied update");
        request.setOrderIndex(1);
        request.setDuration(5);

        assertThrows(AppException.class, () -> lessonService.updateLesson(10L, request));
    }

    @Test
    void deleteLesson_unauthorized_TC_AUTH_001_delete() {
        // TC-AUTH-001: non-admin user cannot delete lesson.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        assertThrows(AppException.class, () -> lessonService.deleteLesson(10L));
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void deleteLesson_notFound_TC_LS_014() {
        // TC-LS-014: admin delete with unknown lesson id should throw.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> lessonService.deleteLesson(999L));
        assertTrue(ex.getMessage().contains("Lesson not found"));
    }
}
