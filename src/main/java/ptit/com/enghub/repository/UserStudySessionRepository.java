package ptit.com.enghub.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ptit.com.enghub.entity.UserStudySession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserStudySessionRepository
        extends JpaRepository<UserStudySession, Long> {

    List<UserStudySession> findByUserId(Long userId);

    List<UserStudySession> findByUserIdAndStartedAtBetween(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<UserStudySession> findTopByUserIdOrderByEndedAtDesc(Long userId);
    Optional<UserStudySession> findTopByUserIdOrderByStartedAtDesc(Long userId);

    boolean existsByUserIdAndStartedAtBetween(
            Long userId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    @Query(value = """
    SELECT 
        EXTRACT(DOW FROM started_at) AS day_of_week,
        activity_type,
        COUNT(id)
    FROM user_study_session
    WHERE started_at >= :fromDate
    GROUP BY day_of_week, activity_type
""", nativeQuery = true)
    List<Object[]> countActivityByDay(@Param("fromDate") LocalDateTime fromDate);

    @Query("""
        SELECT s.skill, COUNT(DISTINCT s.userId)
        FROM UserStudySession s
        WHERE s.skill IS NOT NULL
        GROUP BY s.skill
        ORDER BY COUNT(DISTINCT s.userId) DESC
    """)
    List<Object[]> countDistinctUsersBySkill();

    @Query(value = """
    SELECT 
        EXTRACT(DOW FROM started_at) AS day_of_week,
        activity_type,
        COUNT(id)
    FROM user_study_session
    WHERE started_at >= :fromDate
      AND user_id = :userId
    GROUP BY day_of_week, activity_type
""", nativeQuery = true)
    List<Object[]> countActivityByDay(@Param("fromDate") LocalDateTime fromDate,
                                      @Param("userId") Long userId);

    @Query("""
    SELECT s.skill, SUM(s.durationMinutes)
    FROM UserStudySession s
    WHERE s.userId = :userId
      AND s.skill IS NOT NULL
    GROUP BY s.skill
    ORDER BY SUM(s.durationMinutes) DESC
""")
    List<Object[]> sumDurationBySkill(@Param("userId") Long userId);
}

