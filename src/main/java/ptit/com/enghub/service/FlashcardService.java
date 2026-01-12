package ptit.com.enghub.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ptit.com.enghub.dto.request.AddFlashcardRequest;
import ptit.com.enghub.dto.request.BulkFlashcardRequest;
import ptit.com.enghub.dto.request.FlashcardRequest;
import ptit.com.enghub.dto.response.BulkFlashcardResponse;
import ptit.com.enghub.dto.response.FlashcardResponse;
import ptit.com.enghub.entity.*;
import ptit.com.enghub.mapper.FlashcardMapper;
import ptit.com.enghub.repository.DeckRepository;
import ptit.com.enghub.repository.FlashcardRepository;
import ptit.com.enghub.repository.UserFlashcardProgressRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final DeckRepository deckRepository;
    private final FlashcardMapper flashcardMapper;
    private final UserService userService;
    private final UserFlashcardProgressRepository progressRepository;
    private final AIService aiService;

    // 1. Create Flashcard
    @Transactional
    public FlashcardResponse createFlashcard(FlashcardRequest request) {
        User user = userService.getCurrentUser();
        Flashcard flashcard = flashcardMapper.toEntity(request);

        if (request.getDeckId() != null) {
            Deck deck = deckRepository.findById(request.getDeckId())
                    .orElseThrow(() -> new RuntimeException("Deck not found with id: " + request.getDeckId()));

            DeckFlashcard deckFlashcard = new DeckFlashcard();
            deckFlashcard.setDeck(deck);
            deckFlashcard.setFlashcard(flashcard);

            if (flashcard.getDeckFlashcards() == null) {
                flashcard.setDeckFlashcards(new ArrayList<>());
            }
            flashcard.getDeckFlashcards().add(deckFlashcard);
        }

        Flashcard savedFlashcard = flashcardRepository.save(flashcard);

        UserFlashcardProgress progress = UserFlashcardProgress.builder()
                .userId(user.getId())
                .flashcard(savedFlashcard)
                .easeFactor(2.5)
                .repetitions(0)
                .intervalDays(0)
                .nextReviewAt(null)
                .build();

        progressRepository.save(progress);

        return flashcardMapper.toResponse(savedFlashcard);
    }

    // 2. Update Flashcard
    public FlashcardResponse updateFlashcard(Long id, FlashcardRequest request) {
        Flashcard flashcard = flashcardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flashcard not found with id: " + id));

        flashcard.setTerm(request.getTerm());
        flashcard.setPhonetic(request.getPhonetic());
        flashcard.setDefinition(request.getDefinition());
        flashcard.setPartOfSpeech(request.getPartOfSpeech());
        flashcard.setExampleSentence(request.getExampleSentence());

        // Note: Generally we don't update stats (repetitions, etc.) here unless
        // specified

        Flashcard updatedFlashcard = flashcardRepository.save(flashcard);
        return flashcardMapper.toResponse(updatedFlashcard);
    }

    // 3. Delete Flashcard
    public void deleteFlashcard(Long id) {
        if (!flashcardRepository.existsById(id)) {
            throw new RuntimeException("Flashcard not found with id: " + id);
        }
        flashcardRepository.deleteById(id);
    }

    // 4. Get Flashcard
    public List<FlashcardResponse> getFlashcardsByDeckId(Long deckId) {
        List<Flashcard> list = flashcardRepository.findByDeckId(deckId);

        return list.stream()
                .map(flashcardMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<FlashcardResponse> createFlashcardsFromListWords(BulkFlashcardRequest request) {

        Deck deck = deckRepository.findById(request.getDeckId())
                .orElseThrow(() -> new RuntimeException(
                        "Deck not found with id: " + request.getDeckId()));

        List<BulkFlashcardResponse> externalFlashcards =
                aiService.fetchFlashcards(request.getWord());

        List<Flashcard> flashcards = new ArrayList<>();

        for (BulkFlashcardResponse dto : externalFlashcards) {

            Flashcard flashcard = new Flashcard();
            flashcard.setTerm(dto.getTerm());
            flashcard.setPhonetic(dto.getPhonetic());
            flashcard.setDefinition(dto.getDefinition());
            flashcard.setPartOfSpeech(dto.getPartOfSpeech());
            flashcard.setExampleSentence(dto.getExampleSentence());

            DeckFlashcard deckFlashcard = new DeckFlashcard();
            deckFlashcard.setDeck(deck);
            deckFlashcard.setFlashcard(flashcard);

            flashcard.setDeckFlashcards(List.of(deckFlashcard));

            flashcards.add(flashcard);
        }

        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(flashcards);

        List<UserFlashcardProgress> progresses = savedFlashcards.stream()
                .map(flashcard -> UserFlashcardProgress.builder()
                        .userId(deck.getOwnerId())
                        .flashcard(flashcard)
                        .easeFactor(2.5)
                        .repetitions(0)
                        .intervalDays(0)
                        .nextReviewAt(null)
                        .build())
                .toList();

        progressRepository.saveAll(progresses);

        return savedFlashcards.stream()
                .map(flashcardMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<FlashcardResponse> addFlashcardsFromListWords(AddFlashcardRequest request) {

        User user = userService.getCurrentUser();
        Long userId = user.getId();

        Deck deck = deckRepository
            .findByOwnerIdAndName(userId, "Study Vocabulary")
            .orElseGet(() -> {
                Deck newDeck = new Deck();
                newDeck.setName("Study Vocabulary");
                newDeck.setDescription("Vocabulary collected from lessons and reading activities during your study.");
                newDeck.setOwnerId(userId);
                newDeck.setCreatorId(userId);
                newDeck.setSourceDeckId(null);
                return deckRepository.save(newDeck);
            });

        List<BulkFlashcardResponse> externalFlashcards =
                aiService.fetchFlashcards(request.getWords());

        List<Flashcard> flashcards = new ArrayList<>();

        for (BulkFlashcardResponse dto : externalFlashcards) {

            Flashcard flashcard = new Flashcard();
            flashcard.setTerm(dto.getTerm());
            flashcard.setPhonetic(dto.getPhonetic());
            flashcard.setDefinition(dto.getDefinition());
            flashcard.setPartOfSpeech(dto.getPartOfSpeech());
            flashcard.setExampleSentence(dto.getExampleSentence());

            DeckFlashcard deckFlashcard = new DeckFlashcard();
            deckFlashcard.setDeck(deck);
            deckFlashcard.setFlashcard(flashcard);

            flashcard.setDeckFlashcards(List.of(deckFlashcard));

            flashcards.add(flashcard);
        }

        List<Flashcard> savedFlashcards = flashcardRepository.saveAll(flashcards);


        List<UserFlashcardProgress> progresses = savedFlashcards.stream()
                .map(flashcard -> UserFlashcardProgress.builder()
                        .userId(userId)
                        .flashcard(flashcard)
                        .easeFactor(2.5)
                        .repetitions(0)
                        .intervalDays(0)
                        .nextReviewAt(null)
                        .build())
                .toList();

        progressRepository.saveAll(progresses);

        return savedFlashcards.stream()
                .map(flashcardMapper::toResponse)
                .toList();
    }

}
