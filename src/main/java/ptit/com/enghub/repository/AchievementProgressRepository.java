package ptit.com.enghub.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ptit.com.enghub.entity.AchievementProgress;

import java.util.List;
import java.util.Optional;

public interface AchievementProgressRepository
        extends JpaRepository<AchievementProgress, Long> {

    Optional<AchievementProgress> findByUserIdAndAchievementId(
            Long userId, Long achievementId);

    @Query("""
        select p
        from AchievementProgress p
        where p.userId = :userId
          and p.currentValue < p.targetValue
    """)
    List<AchievementProgress> findInProgress(@Param("userId") Long userId);

    @Query("""
        select p
        from AchievementProgress p
        where p.userId = :userId
          and p.currentValue >= p.targetValue
    """)
    List<AchievementProgress> findCompletedProgress(@Param("userId") Long userId);

    @Modifying
    @Query(value = "CALL reset_streak_if_missed(:userId)", nativeQuery = true)
    void resetStreakIfMissed(@Param("userId") Long userId);


}