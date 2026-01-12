package ptit.com.enghub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.com.enghub.dto.request.UnitRequest;
import ptit.com.enghub.dto.response.LessonResponse;
import ptit.com.enghub.dto.response.UnitResponse;
import ptit.com.enghub.entity.Course;
import ptit.com.enghub.entity.Unit;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.UserProgress;
import ptit.com.enghub.enums.Level;
import ptit.com.enghub.mapper.UnitMapper;
import ptit.com.enghub.repository.CourseRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.repository.UserProgressRepository;
import ptit.com.enghub.service.IService.UnitService;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;
    private final UnitMapper unitMapper;
    private final CourseRepository courseRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserService userService;

    @Override
    public UnitResponse getUnitById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        return unitMapper.toResponse(unit);
    }

    @Override
    public List<UnitResponse> getUnitsByCourseId(Long courseId) {
        User user = userService.getCurrentUser();
        List<UnitResponse> units = unitRepository.findByCourse_Id(courseId).stream()
                .map(unitMapper::toResponse)
                .peek(unit -> {
                    unit.setLessons(
                            unit.getLessons().stream()
                                    .sorted(Comparator.comparing(LessonResponse::getOrderIndex))
                                    .peek(l -> {
                                        boolean completed = userProgressRepository
                                                .findByUserIdAndLessonId(user.getId(), l.getId())
                                                .map(UserProgress::isCompleted)
                                                .orElse(false);
                                        l.setCompleted(completed);
                                    })
                                    .collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
        return units;
    }

//    @Override
//    public List<UnitResponse> getUnitsByCourseId(Long courseId) {
//        User user = userService.getCurrentUser();
//        Level userLevel = user.getLevel();
//
//        Course course = courseRepository.findById(courseId)
//                .orElseThrow(() -> new RuntimeException("Unit not found"));;
//
//        String courseLevelStr = course.getLevel();
//        Level courseLevel;
//        try {
//            courseLevel = Level.valueOf(courseLevelStr.toUpperCase());
//        } catch (IllegalArgumentException | NullPointerException e) {
//            courseLevel = null;
//        }
//
//        boolean canAccessCourse = canAccessCourse(userLevel, courseLevel);
//
//        List<UnitResponse> units = unitRepository.findByCourse_Id(courseId).stream()
//                .map(unitMapper::toResponse)
//                .peek(unit -> {
//
//                    List<LessonResponse> lessons = unit.getLessons().stream()
//                            .sorted(Comparator.comparingInt(LessonResponse::getOrderIndex))
//                            .collect(Collectors.toList());
//
//                    for (LessonResponse l : lessons) {
//                        boolean completed = userProgressRepository
//                                .findByUserIdAndLessonId(user.getId(), l.getId())
//                                .map(UserProgress::isCompleted)
//                                .orElse(false);
//                        l.setCompleted(completed);
//                        l.setUnlock(completed);
//                        log.info(String.valueOf(completed));
//                    }
//
//                    if (!canAccessCourse || lessons.isEmpty()) {
//                        unit.setLessons(lessons);
//                        return;
//                    }
//
//                    OptionalInt lastCompletedIndexOpt = java.util.stream.IntStream
//                            .range(0, lessons.size())
//                            .filter(i -> lessons.get(i).isCompleted())
//                            .max();
//
//                    int unlockIndex;
//                    if (lastCompletedIndexOpt.isPresent()) {
//                        int lastCompletedIndex = lastCompletedIndexOpt.getAsInt();
//                        if (lastCompletedIndex + 1 < lessons.size()) {
//                            unlockIndex = lastCompletedIndex + 1;
//                        } else {
//                            unlockIndex = lastCompletedIndex;
//                        }
//                    } else {
//                        unlockIndex = 0;
//                    }
//
//                    lessons.get(unlockIndex).setUnlock(true);
//
//                    unit.setLessons(lessons);
//                })
//                .collect(Collectors.toList());
//
//        return units;
//    }
//
//    private boolean canAccessCourse(Level userLevel, Level courseLevel) {
//        if (userLevel == null || courseLevel == null) return false;
//
//        // BEGINNER < INTERMEDIATE < ADVANCED < PROFICIENCY
//        return userLevel.ordinal() >= courseLevel.ordinal();
//    }


    @Override
    @Transactional
    public UnitResponse createUnit(UnitRequest request) {
        Unit unit = unitMapper.toEntity(request);

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        unit.setCourse(course);
        unit.setIcon(request.getIcon());
        unit.setDescription(request.getDescription());
        unit.setOrderIndex(request.getOrderIndex());
        unit.setTitle(request.getTitle());
        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public UnitResponse updateUnit(Long id, UnitRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        unitMapper.updateUnitFromRequest(request, unit);

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            unit.setCourse(course);
        }
        unit.setIcon(request.getIcon());
        unit.setDescription(request.getDescription());
        unit.setOrderIndex(request.getOrderIndex());
        unit.setTitle(request.getTitle());

        return unitMapper.toResponse(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public void deleteUnit(Long id) {
        if (!unitRepository.existsById(id)) {
            throw new RuntimeException("Unit not found");
        }
        unitRepository.deleteById(id);
    }
}
