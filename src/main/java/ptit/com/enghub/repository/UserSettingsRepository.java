package ptit.com.enghub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.UserLearningSettings;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface UserSettingsRepository
        extends JpaRepository<UserLearningSettings, Long> {

    @Query("""
        select p
        from UserLearningSettings p
        join fetch p.user u
        where p.dailyStudyReminder = true
          and p.emailNotification = true
          and p.reminderTime = :time
          and u.status = 'ACTIVE'
    """)
    List<UserLearningSettings> findUsersNeedDailyEmail(LocalTime time);
    Optional<UserLearningSettings> findByUser(User user);

    Optional<UserLearningSettings> findByUserId(Long userId);
}