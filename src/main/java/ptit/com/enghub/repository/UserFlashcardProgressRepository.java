package ptit.com.enghub.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ptit.com.enghub.entity.UserFlashcardProgress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserFlashcardProgressRepository
        extends JpaRepository<UserFlashcardProgress, Long> {

    Optional<UserFlashcardProgress>
    findByUserIdAndFlashcardId(Long userId, Long flashcardId);

    List<UserFlashcardProgress>
    findByUserIdAndNextReviewAtBefore(
            Long userId,
            LocalDateTime time
    );

    @Modifying
    @Query("""
        delete from UserFlashcardProgress p
        where p.userId = :userId
          and p.flashcard.id in :flashcardIds
    """)
    void deleteByUserIdAndFlashcardIds(
            @Param("userId") Long userId,
            @Param("flashcardIds") List<Long> flashcardIds
    );

    @Query("""
        select p
        from UserFlashcardProgress p
        where p.userId = :userId
          and p.flashcard.id in :flashcardIds
    """)
    List<UserFlashcardProgress> findByUserIdAndFlashcardIdIn(
            @Param("userId") Long userId,
            @Param("flashcardIds") List<Long> flashcardIds
    );

    @Query("""
        SELECT p
        FROM UserFlashcardProgress p
        JOIN p.flashcard f
        JOIN DeckFlashcard df ON df.flashcard = f
        WHERE p.userId = :userId
          AND df.deck.id = :deckId
          AND p.nextReviewAt IS NOT NULL
          AND p.nextReviewAt <= :now
        ORDER BY p.nextReviewAt ASC
    """)
    List<UserFlashcardProgress> findDueCards(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );


    @Query("""
        SELECT p
        FROM UserFlashcardProgress p
        JOIN p.flashcard f
        JOIN DeckFlashcard df ON df.flashcard = f
        WHERE p.userId = :userId
          AND df.deck.id = :deckId
          AND p.repetitions = 0
    """)
    List<UserFlashcardProgress> findNewCards(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            Pageable pageable
    );

    void deleteByUserIdAndFlashcardIdIn(
            Long userId,
            List<Long> flashcardIds
    );

    @Modifying
    @Query("""
        UPDATE UserFlashcardProgress p
        SET p.easeFactor = 2.5,
            p.repetitions = 0,
            p.intervalDays = 0,
            p.nextReviewAt = NULL,
            p.lastReviewedAt = NULL
        WHERE p.userId = :userId
          AND p.flashcard.id IN :flashcardIds
    """)
    void resetProgress(
            @Param("userId") Long userId,
            @Param("flashcardIds") List<Long> flashcardIds
    );

    int countByUserIdAndLastReviewedAtIsNotNull(Long userId);
}