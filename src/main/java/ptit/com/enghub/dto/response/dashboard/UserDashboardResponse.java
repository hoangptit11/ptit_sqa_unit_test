package ptit.com.enghub.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardResponse {

    int streakDays;

    int targetDailyStudied;
    int targetDaysPerW;

    int lessonComplete;
    int flashcardsStudied;

    int timeStudyToday;
    int timeStudyW;
    int timeStudyM;
    private List<ActivityDataResponse> activityData;
    private List<ActivitySkillUserResponse> activityDataSkill;
}
