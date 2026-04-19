package ptit.com.enghub.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ptit.com.enghub.dto.LessonSeedData;
import ptit.com.enghub.dto.VocabularyDTO;
import ptit.com.enghub.dto.request.AddFlashcardRequest;
import ptit.com.enghub.dto.response.BulkFlashcardResponse;
import ptit.com.enghub.dto.response.FlashcardResponse;
import ptit.com.enghub.entity.*;
import ptit.com.enghub.mapper.FlashcardMapper;
import ptit.com.enghub.mapper.LessonMapper;
import ptit.com.enghub.repository.*;
import ptit.com.enghub.service.AIService;
import ptit.com.enghub.service.FlashcardService;
import ptit.com.enghub.service.LessonServiceImpl;
import ptit.com.enghub.service.NotificationService;
import ptit.com.enghub.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VocabularyAndFlashcardServiceTest {

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

    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private FlashcardMapper flashcardMapper;
    @Mock
    private UserFlashcardProgressRepository flashcardProgressRepository;
    @Mock
    private AIService aiService;

    private FlashcardService flashcardService;

    @BeforeEach
    void initFlashcardService() {
        flashcardService = new FlashcardService(
                flashcardRepository,
                deckRepository,
                flashcardMapper,
                userService,
                flashcardProgressRepository,
                aiService
        );
    }

    @Test
    void seedLesson_replaceVocabularyList_TC_VC_011() {
        // TC-VC-011: seedLesson should clear old vocabulary then add new rows.
        Lesson lesson = new Lesson();
        lesson.setTitle("Lesson 1");
        Vocabulary oldVocab = new Vocabulary();
        oldVocab.setWord("old");
        oldVocab.setMeaning("old meaning");
        oldVocab.setLesson(lesson);
        lesson.getVocabularies().add(oldVocab);

        LessonSeedData seedData = new LessonSeedData();
        VocabularyDTO dto = new VocabularyDTO();
        dto.setWord("apple");
        dto.setMeaning("qua tao");
        dto.setExample("I eat an apple");
        seedData.setVocabularies(List.of(dto));

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));

        lessonService.seedLesson(1L, seedData);

        // CheckDB: save is called with replaced vocabulary collection.
        assertEquals(1, lesson.getVocabularies().size());
        assertEquals("apple", lesson.getVocabularies().get(0).getWord());
        verify(lessonRepository).save(lesson);
    }

    @Test
    void addFlashcards_autoCreateDeckAndProgress_TC_FC_003() {
        // TC-FC-003: when "Study Vocabulary" deck is missing, service must create it.
        User currentUser = new User();
        currentUser.setId(10L);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(deckRepository.findByOwnerIdAndName(10L, "Study Vocabulary")).thenReturn(Optional.empty());

        Deck createdDeck = new Deck();
        createdDeck.setId(100L);
        createdDeck.setOwnerId(10L);
        when(deckRepository.save(any(Deck.class))).thenReturn(createdDeck);

        BulkFlashcardResponse aiCard = new BulkFlashcardResponse();
        aiCard.setTerm("apple");
        aiCard.setDefinition("qua tao");
        when(aiService.fetchFlashcards(any())).thenReturn(List.of(aiCard));

        Flashcard persisted = new Flashcard();
        persisted.setId(999L);
        when(flashcardRepository.saveAll(any())).thenReturn(List.of(persisted));

        FlashcardResponse mapped = new FlashcardResponse();
        mapped.setId(999L);
        when(flashcardMapper.toResponse(any(Flashcard.class))).thenReturn(mapped);

        AddFlashcardRequest request = new AddFlashcardRequest();
        request.setWords(List.of("apple"));
        List<FlashcardResponse> result = flashcardService.addFlashcardsFromListWords(request);

        // CheckDB: verify deck creation and progress persistence are triggered.
        assertEquals(1, result.size());
        verify(deckRepository).save(any(Deck.class));
        verify(flashcardProgressRepository).saveAll(any());

        ArgumentCaptor<List<UserFlashcardProgress>> progressCaptor = ArgumentCaptor.forClass(List.class);
        verify(flashcardProgressRepository).saveAll(progressCaptor.capture());
        assertEquals(10L, progressCaptor.getValue().get(0).getUserId());
    }

    @Test
    void addFlashcards_useExistingDeck_TC_FC_004() {
        // TC-FC-004: existing "Study Vocabulary" deck should be reused.
        User currentUser = new User();
        currentUser.setId(15L);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        Deck existingDeck = new Deck();
        existingDeck.setId(500L);
        existingDeck.setOwnerId(15L);
        when(deckRepository.findByOwnerIdAndName(15L, "Study Vocabulary")).thenReturn(Optional.of(existingDeck));

        BulkFlashcardResponse aiCard = new BulkFlashcardResponse();
        aiCard.setTerm("book");
        aiCard.setDefinition("sach");
        when(aiService.fetchFlashcards(any())).thenReturn(List.of(aiCard));

        Flashcard persisted = new Flashcard();
        persisted.setId(777L);
        when(flashcardRepository.saveAll(any())).thenReturn(List.of(persisted));

        FlashcardResponse mapped = new FlashcardResponse();
        mapped.setId(777L);
        when(flashcardMapper.toResponse(any(Flashcard.class))).thenReturn(mapped);

        AddFlashcardRequest request = new AddFlashcardRequest();
        request.setWords(List.of("book"));
        List<FlashcardResponse> result = flashcardService.addFlashcardsFromListWords(request);

        assertEquals(1, result.size());
        verify(deckRepository, never()).save(any(Deck.class));
        verify(flashcardProgressRepository).saveAll(any());
    }
}
