package ptit.com.enghub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ptit.com.enghub.dto.LessonSeedData;
import ptit.com.enghub.dto.request.CompleteLessonRequest;
import ptit.com.enghub.dto.request.LessonCreationRequest;
import ptit.com.enghub.dto.request.NotificationRequest;
import ptit.com.enghub.dto.response.LessonResponse;
import ptit.com.enghub.entity.*;
import ptit.com.enghub.enums.Level;
import ptit.com.enghub.enums.NotificationType;
import ptit.com.enghub.enums.StudySkill;
import ptit.com.enghub.exception.AppException;
import ptit.com.enghub.exception.ErrorCode;
import ptit.com.enghub.mapper.LessonMapper;
import ptit.com.enghub.repository.LessonRepository;
import ptit.com.enghub.repository.UnitRepository;
import ptit.com.enghub.repository.UserProgressRepository;
import ptit.com.enghub.repository.UserRepository;
import ptit.com.enghub.service.IService.LessonService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final UserProgressRepository userProgressRepository;
    private final LessonMapper lessonMapper;
    private final UnitRepository unitRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;


    @Override
    public LessonResponse getLesson(Long lessonId) {
        User user = userService.getCurrentUser();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        LessonResponse response = lessonMapper.toResponse(lesson);

        boolean completed = userProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .map(UserProgress::isCompleted)
                .orElse(false);

        response.setCompleted(completed);
        return response;
    }

    @Override
    public List<LessonResponse> getLessonsByUnitId(Long unitId) {
        return lessonRepository.findByUnit_Id(unitId).stream()
                .map(lessonMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void completeLesson(Long lessonId, CompleteLessonRequest request) {
        User user = userService.getCurrentUser();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        UserProgress progress = userProgressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(new UserProgress());

        progress.setUserId(user.getId());
        progress.setLesson(lesson);
        progress.setCompleted(true);
        progress.setScore(request.getScore());
        progress.setLastUpdated(LocalDateTime.now());

        userProgressRepository.save(progress);

        updateUserLevel(user.getId());

        NotificationRequest n = new NotificationRequest();
        n.setUserId(user.getId().toString());
        n.setType(NotificationType.SYSTEM_MESSAGE);
        n.setTitle("Hoàn thành lesson");
        n.setContent(
                "Bạn đã hoàn thành một bài học. Hãy tiếp tục bài tiếp theo để giữ vững đà học nhé!"
        );
        notificationService.create(n);

        unlockNextLesson(lesson, user.getId());
    }

    public void updateUserLevel(Long userId) {

        boolean has21 = userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(userId, 21L);
        boolean has22 = userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(userId, 22L);
        boolean has23 = userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(userId, 23L);
        boolean has24 = userProgressRepository.existsByUserIdAndLessonIdAndCompletedTrue(userId, 24L);
        User user = userService.getCurrentUser();

        if (has21 && has22 && user.getLevel() == Level.BEGINNER) {
            user.setLevel(Level.INTERMEDIATE);
        }

        if (has23 && has24 && user.getLevel() != Level.ADVANCED) {
            user.setLevel(Level.ADVANCED);
        }

        userRepository.save(user);
    }

    private boolean isLessonUnlocked(Lesson lesson) {
        User user = userService.getCurrentUser();
        if (lesson.getOrderIndex() == 0) {
            return true;
        }

        // Kiểm tra bài học trước đó
        Lesson previousLesson = lessonRepository.findByUnit_Id(lesson.getUnit().getId()).stream()
                .filter(l -> l.getOrderIndex() == lesson.getOrderIndex() - 1)
                .findFirst()
                .orElse(null);

        if (previousLesson == null) {
            return true;
        }

        return userProgressRepository.findByUserIdAndLessonId(user.getId(), previousLesson.getId())
                .map(UserProgress::isCompleted)
                .orElse(false);
    }

    private void unlockNextLesson(Lesson currentLesson, Long userId) {
        Lesson nextLesson = lessonRepository.findByUnit_Id(currentLesson.getUnit().getId()).stream()
                .filter(l -> l.getOrderIndex() == currentLesson.getOrderIndex() + 1)
                .findFirst()
                .orElse(null);

        if (nextLesson != null) {
            UserProgress nextProgress = userProgressRepository.findByUserIdAndLessonId(userId, nextLesson.getId())
                    .orElse(new UserProgress());
            nextProgress.setUserId(userId);
            nextProgress.setLesson(nextLesson);
            nextProgress.setCompleted(false);
            nextProgress.setScore(0);
            nextProgress.setLastUpdated(LocalDateTime.now());
            userProgressRepository.save(nextProgress);
        }
    }

    @Transactional
    public Lesson createLesson(LessonCreationRequest request) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setDuration(request.getDuration());
        lesson.setUnit(unit);
        lesson.setStudySkill(request.getStudySkill());
        if (request.getVideo() != null) {
            Video video = new Video();
            video.setUrl(request.getVideo().getUrl());
            video.setDescription(request.getVideo().getDescription());
            video.setDuration(request.getVideo().getDuration());
            video.setLesson(lesson);
            lesson.setVideo(video);
        }

        if (request.getVocabularies() != null) {
            List<Vocabulary> vocabList = request.getVocabularies().stream()
                    .map(v -> {
                        Vocabulary vo = new Vocabulary();
                        vo.setWord(v.getWord());
                        vo.setMeaning(v.getMeaning());
                        vo.setExample(v.getExample());
                        vo.setImageUrl(v.getImageUrl());
                        vo.setLesson(lesson);
                        return vo;
                    }).toList();
            lesson.setVocabularies(vocabList);
        }

        if (request.getDialogues() != null) {
            List<Dialogue> dList = request.getDialogues().stream()
                    .map(d -> {
                        Dialogue di = new Dialogue();
                        di.setSpeaker(d.getSpeaker());
                        di.setText(d.getText());
                        di.setLesson(lesson);
                        return di;
                    }).toList();
            lesson.setDialogues(dList);
        }

        if (request.getGrammar() != null) {
            Grammar grammar = new Grammar();
            grammar.setLesson(lesson);
            grammar.setTopic(request.getGrammar().getTopic());
            grammar.setExplanation(request.getGrammar().getExplanation());
            grammar.setSignalWord(request.getGrammar().getSignalWord());

            List<GrammarFormula> formulas = request.getGrammar().getFormulas()
                    .stream().map(f -> {
                        GrammarFormula gf = new GrammarFormula();
                        gf.setGrammar(grammar);
                        gf.setType(f.getType());
                        gf.setFormula(f.getFormula());
                        gf.setDescription(f.getDescription());
                        gf.setVerbType(f.getVerbType());

                        List<GrammarExample> examples = f.getExamples().stream()
                                .map(e -> {
                                    GrammarExample ex = new GrammarExample();
                                    ex.setFormula(gf);
                                    ex.setSentence(e.getSentence());
                                    ex.setTranslation(e.getTranslation());
                                    ex.setHighlight(e.getHighlight());
                                    return ex;
                                }).toList();

                        gf.setExamples(examples);
                        return gf;
                    }).toList();

            grammar.setFormulas(formulas);
            lesson.setGrammar(grammar);
        }

        if (request.getExercises() != null) {
            List<Exercise> exList = request.getExercises().stream()
                    .map(e -> {
                        Exercise ex = new Exercise();
                        ex.setQuestion(e.getQuestion());
                        ex.setType(e.getType());
                        ex.setLesson(lesson);


                        try {
                            JsonNode metadataNode = e.getMetadata();
                            if (metadataNode != null && !metadataNode.isNull()) {
                                ex.setMetadata(objectMapper.writeValueAsString(metadataNode));
                            } else {
                                ex.setMetadata(null);
                            }
                        } catch (Exception ex1) {
                            ex.setMetadata(null);
                        }


                        return ex;
                    }).toList();
            lesson.setExercises(exList);
        }

        return lessonRepository.save(lesson);
    }

    @Override
    public void deleteLesson(Long id) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void seedLesson(Long lessonId, LessonSeedData data){
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        if (data.getVideo() != null) {
            Video video = new Video();
            video.setUrl(data.getVideo().getUrl());
            video.setDescription(data.getVideo().getDescription());
            video.setDuration(data.getVideo().getDuration());
            video.setLesson(lesson);
            lesson.setVideo(video);
        }

        if (data.getVocabularies() != null) {
            lesson.getVocabularies().clear();

            for (var v : data.getVocabularies()) {
                Vocabulary vo = new Vocabulary();
                vo.setWord(v.getWord());
                vo.setMeaning(v.getMeaning());
                vo.setExample(v.getExample());
                vo.setImageUrl(v.getImageUrl());
                vo.setLesson(lesson);

                lesson.getVocabularies().add(vo);
            }
        }


        if (data.getDialogues() != null) {
            lesson.getDialogues().clear();

            for (var d : data.getDialogues()) {
                Dialogue di = new Dialogue();
                di.setSpeaker(d.getSpeaker());
                di.setText(d.getText());
                di.setLesson(lesson);

                lesson.getDialogues().add(di);
            }
        }


        if (data.getGrammar() != null) {

            Grammar grammar = lesson.getGrammar();
            if (grammar == null) {
                grammar = new Grammar();
                grammar.setLesson(lesson);
                lesson.setGrammar(grammar);
            }

            grammar.setTopic(data.getGrammar().getTopic());
            grammar.setExplanation(data.getGrammar().getExplanation());
            grammar.setSignalWord(data.getGrammar().getSignalWord());

            grammar.getFormulas().clear();

            for (var f : data.getGrammar().getFormulas()) {
                GrammarFormula gf = new GrammarFormula();
                gf.setGrammar(grammar);
                gf.setType(f.getType());
                gf.setFormula(f.getFormula());
                gf.setDescription(f.getDescription());
                gf.setVerbType(f.getVerbType());

                gf.getExamples().clear();

                for (var e : f.getExamples()) {
                    GrammarExample ex = new GrammarExample();
                    ex.setFormula(gf);
                    ex.setSentence(e.getSentence());
                    ex.setTranslation(e.getTranslation());
                    ex.setHighlight(e.getHighlight());

                    gf.getExamples().add(ex);
                }

                grammar.getFormulas().add(gf);
            }
        }


        if (data.getExercises() != null) {
            lesson.getExercises().clear();

            for (var e : data.getExercises()) {
                Exercise ex = new Exercise();
                ex.setQuestion(e.getQuestion());
                ex.setType(e.getType());
                ex.setLesson(lesson);

                try {
                    if (e.getMetadata() != null && !e.getMetadata().isNull()) {
                        ex.setMetadata(objectMapper.writeValueAsString(e.getMetadata()));
                    }
                } catch (Exception ignore) {}

                lesson.getExercises().add(ex);
            }
        }


        lessonRepository.save(lesson);
    }

    @Override
    public LessonResponse updateLesson(Long id, LessonCreationRequest request) {

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lesson not found with id: " + id));

        lesson.setTitle(request.getTitle());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setDuration(request.getDuration());
        lesson.setStudySkill(request.getStudySkill());

        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            lesson.setUnit(unit);
        }

        lesson.setVideo(null);
        if (request.getVideo() != null) {
            Video video = new Video();
            video.setUrl(request.getVideo().getUrl());
            video.setDescription(request.getVideo().getDescription());
            video.setDuration(request.getVideo().getDuration());
            video.setLesson(lesson);
            lesson.setVideo(video);
        }

        lesson.getVocabularies().clear();
        if (request.getVocabularies() != null) {
            List<Vocabulary> vocabList = request.getVocabularies().stream()
                    .map(v -> {
                        Vocabulary vo = new Vocabulary();
                        vo.setWord(v.getWord());
                        vo.setMeaning(v.getMeaning());
                        vo.setExample(v.getExample());
                        vo.setImageUrl(v.getImageUrl());
                        vo.setLesson(lesson);
                        return vo;
                    }).toList();
            lesson.getVocabularies().addAll(vocabList);
        }

        lesson.getDialogues().clear();
        if (request.getDialogues() != null) {
            List<Dialogue> dList = request.getDialogues().stream()
                    .map(d -> {
                        Dialogue di = new Dialogue();
                        di.setSpeaker(d.getSpeaker());
                        di.setText(d.getText());
                        di.setLesson(lesson);
                        return di;
                    }).toList();
            lesson.getDialogues().addAll(dList);
        }

        lesson.setGrammar(null);
        if (request.getGrammar() != null) {
            Grammar grammar = new Grammar();
            grammar.setLesson(lesson);
            grammar.setTopic(request.getGrammar().getTopic());
            grammar.setExplanation(request.getGrammar().getExplanation());
            grammar.setSignalWord(request.getGrammar().getSignalWord());

            List<GrammarFormula> formulas = request.getGrammar().getFormulas()
                    .stream().map(f -> {
                        GrammarFormula gf = new GrammarFormula();
                        gf.setGrammar(grammar);
                        gf.setType(f.getType());
                        gf.setFormula(f.getFormula());
                        gf.setDescription(f.getDescription());
                        gf.setVerbType(f.getVerbType());

                        List<GrammarExample> examples = f.getExamples().stream()
                                .map(e -> {
                                    GrammarExample ex = new GrammarExample();
                                    ex.setFormula(gf);
                                    ex.setSentence(e.getSentence());
                                    ex.setTranslation(e.getTranslation());
                                    ex.setHighlight(e.getHighlight());
                                    return ex;
                                }).toList();

                        gf.setExamples(examples);
                        return gf;
                    }).toList();

            grammar.setFormulas(formulas);
            lesson.setGrammar(grammar);
        }

        // ===== Exercises (1-N) =====
        lesson.getExercises().clear();
        if (request.getExercises() != null) {
            List<Exercise> exList = request.getExercises().stream()
                    .map(e -> {
                        Exercise ex = new Exercise();
                        ex.setQuestion(e.getQuestion());
                        ex.setType(e.getType());
                        ex.setLesson(lesson);

                        try {
                            JsonNode metadataNode = e.getMetadata();
                            if (metadataNode != null && !metadataNode.isNull()) {
                                ex.setMetadata(objectMapper.writeValueAsString(metadataNode));
                            }
                        } catch (Exception ignored) {}

                        return ex;
                    }).toList();

            lesson.getExercises().addAll(exList);
        }

        Lesson savedLesson = lessonRepository.save(lesson);
        return lessonMapper.toResponse(savedLesson);
    }

}
