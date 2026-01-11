package ptit.com.enghub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ptit.com.enghub.entity.UserProgress;

import java.util.Optional;

@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    Optional<UserProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    int countByUserIdAndCompletedTrue(Long userId);
    boolean existsByUserIdAndLessonIdAndCompletedTrue(Long userId, Long lessonId);
}
