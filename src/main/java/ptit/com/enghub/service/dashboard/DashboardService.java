package ptit.com.enghub.service.dashboard;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ptit.com.enghub.dto.response.dashboard.*;
import ptit.com.enghub.entity.AchievementProgress;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.UserLearningSettings;
import ptit.com.enghub.enums.StudyActivityType;
import ptit.com.enghub.enums.StudySkill;
import ptit.com.enghub.repository.*;
import ptit.com.enghub.service.UserService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final SkillRepository skillRepository;
    private final DeckRepository deckRepository;
    private final UserStudySessionRepository sessionRepository;
    private final UserService userService;
    private final UserStudyDailyRepository dailyRepository;
    private final UserFlashcardProgressRepository userFlashcardProgressRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserSettingsRepository userLearningSettingsRepository;
    private final AchievementProgressRepository achievementProgressRepository;

    public AdminDashboardResponse getDashboard() {

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long totalLessons = lessonRepository.count();
        long totalSkills = skillRepository.count();
        long totalDeck = deckRepository.count();

        List<ActivityDataResponse> activityDataResponses = getWeeklyActivityData();
        List<ActivitySkillResponse> activitySkillData = getActivitySkillData();
        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalLessons(totalLessons)
                .totalSkills(totalSkills)
                .activeUsers(activeUsers)
                .totalDeck(totalDeck)
                .activityData(activityDataResponses)
                .activityDataSkill(activitySkillData)
                .build();
    }

    public List<ActivityDataResponse> getWeeklyActivityData() {

        LocalDateTime mondayStart = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        List<Object[]> rawData =
                sessionRepository.countActivityByDay(mondayStart);

        Map<Integer, ActivityDataResponse> map = new HashMap<>();

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        int todayValue = today.getValue();
        for (int i = 1; i <= todayValue; i++) {
            map.put(i, new ActivityDataResponse("T" + (i + 1), 0, 0, 0));
        }

        for (Object[] row : rawData) {
            Integer dow = ((Number) row[0]).intValue();

            StudyActivityType type =
                    StudyActivityType.valueOf(row[1].toString());

            int count = ((Number) row[2]).intValue();

            ActivityDataResponse data = map.get(dow);
            if (data == null) continue;

            switch (type) {
                case FLASHCARD -> data.setFlashcards(count);
                case LESSON -> data.setLessons(count);
                case SKILL -> data.setSkills(count);
            }
        }

        return map.values()
                .stream()
                .sorted(Comparator.comparing(ActivityDataResponse::getName))
                .toList();
    }

    public List<ActivitySkillResponse> getActivitySkillData() {

        Map<StudySkill, Integer> skillMap = new EnumMap<>(StudySkill.class);
        for (StudySkill skill : StudySkill.values()) {
            skillMap.put(skill, 0);
        }

        sessionRepository.countDistinctUsersBySkill()
                .forEach(row -> {
                    StudySkill skill = (StudySkill) row[0];
                    int count = ((Long) row[1]).intValue();
                    skillMap.put(skill, count);
                });
        return skillMap.entrySet().stream()
                .map(e -> new ActivitySkillResponse(
                        e.getKey().name(),
                        e.getValue()
                ))
                .toList();
    }


    public UserDashboardResponse getDashboardUser() {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        Integer minutesToday = dailyRepository
                .sumTotalMinutesByUserAndDateBetween(user.getId(), today, today);

        Integer minutesWeek = dailyRepository
                .sumTotalMinutesByUserAndDateBetween(user.getId(), startOfWeek, today);

        Integer minutesMonth = dailyRepository
                .sumTotalMinutesByUserAndDateBetween(user.getId(), startOfMonth, today);
        int timeToday = minutesToday == null ? 0 : minutesToday;
        int timeWeek = minutesWeek == null ? 0 : minutesWeek;
        int timeMonth = minutesMonth == null ? 0 : minutesMonth;

        int lessonComplete = userProgressRepository.countByUserIdAndCompletedTrue(user.getId());
        int flashcardsStudied = userFlashcardProgressRepository
                .countByUserIdAndLastReviewedAtIsNotNull(user.getId());


        UserLearningSettings settings = userLearningSettingsRepository
                .findByUserId(user.getId())
                .orElse(null);
        int targetDailyStudied = 0;
        int targetDaysPerW = 0;

        if (settings != null) {
            if (settings.getDailyStudyMinutes() != null) {
                targetDailyStudied = settings.getDailyStudyMinutes();
            }
            if (settings.getTargetDaysPerWeek() != null) {
                targetDaysPerW = settings.getTargetDaysPerWeek();
            }
        }

        int streakDays = achievementProgressRepository
                .findByUserIdAndAchievementId(user.getId(), 1L)
                .map(AchievementProgress::getCurrentValue)
                .orElse(0);

        List<ActivityDataResponse> activityDataResponses = getWeeklyActivityDataUser(user.getId());
        List<ActivitySkillUserResponse> activitySkillData = getActivitySkillDataUser(user.getId());
        return UserDashboardResponse.builder()
                .streakDays(streakDays)
                .targetDailyStudied(targetDailyStudied)
                .targetDaysPerW(targetDaysPerW)
                .lessonComplete(lessonComplete)
                .flashcardsStudied(flashcardsStudied)
                .timeStudyToday(timeToday)
                .timeStudyW(timeWeek)
                .timeStudyM(timeMonth)
                .activityData(activityDataResponses)
                .activityDataSkill(activitySkillData)
                .build();
    }

    public List<ActivityDataResponse> getWeeklyActivityDataUser(Long userId) {
        LocalDateTime mondayStart = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        List<Object[]> rawData =
                sessionRepository.countActivityByDay(mondayStart, userId);

        Map<Integer, ActivityDataResponse> map = new HashMap<>();

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        int todayValue = today.getValue();
        for (int i = 1; i <= todayValue; i++) {
            map.put(i, new ActivityDataResponse("T" + (i + 1), 0, 0, 0));
        }

        for (Object[] row : rawData) {
            Integer dow = ((Number) row[0]).intValue();

            StudyActivityType type =
                    StudyActivityType.valueOf(row[1].toString());

            int count = ((Number) row[2]).intValue();

            ActivityDataResponse data = map.get(dow);
            if (data == null) continue;

            switch (type) {
                case FLASHCARD -> data.setFlashcards(count);
                case LESSON -> data.setLessons(count);
                case SKILL -> data.setSkills(count);
            }
        }

        return map.values()
                .stream()
                .sorted(Comparator.comparing(ActivityDataResponse::getName))
                .toList();
    }

    public List<ActivitySkillUserResponse> getActivitySkillDataUser(Long userId) {

        Map<StudySkill, Integer> skillMap = new EnumMap<>(StudySkill.class);
        for (StudySkill skill : StudySkill.values()) {
            skillMap.put(skill, 0);
        }

        sessionRepository.sumDurationBySkill(userId)
                .forEach(row -> {
                    StudySkill skill = (StudySkill) row[0];
                    int count = ((Long) row[1]).intValue();
                    skillMap.put(skill, count);
                });
        return skillMap.entrySet().stream()
                .map(e -> new ActivitySkillUserResponse(
                        e.getKey().name(),
                        e.getValue()
                ))
                .toList();
    }
}
