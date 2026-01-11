package ptit.com.enghub.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ptit.com.enghub.entity.UserStudyDaily;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserStudyDailyRepository
        extends JpaRepository<UserStudyDaily, Long> {

    Optional<UserStudyDaily> findByUserIdAndStudyDate(
            Long userId,
            LocalDate studyDate
    );

    @Query("""
        SELECT COALESCE(d.totalMinutes, 0)
        FROM UserStudyDaily d
        WHERE d.userId = :userId
        AND d.studyDate = CURRENT_DATE
    """)
    Integer getTodayMinutes(@Param("userId") Long userId);

    @Query("""
        SELECT MAX(d.lastStudyAt)
        FROM UserStudyDaily d
        WHERE d.userId = :userId
    """)
    LocalDateTime findLastStudyAt(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(d)
        FROM UserStudyDaily d
        WHERE d.userId = :userId
        AND d.studyDate <= CURRENT_DATE
    """)
    long countStudyDays(@Param("userId") Long userId);

    @Query("""
        SELECT d.studyDate, d.totalMinutes
        FROM UserStudyDaily d
        WHERE d.userId = :userId
          AND d.studyDate >= :fromDate
        ORDER BY d.studyDate
    """)
    List<Object[]> findDailyStudyMinutes(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        SELECT COALESCE(SUM(u.totalMinutes), 0)
        FROM UserStudyDaily u
        WHERE u.userId = :userId
          AND u.studyDate BETWEEN :from AND :to
    """)
    Integer sumTotalMinutesByUserAndDateBetween(@Param("userId") Long userId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

}
