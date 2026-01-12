package ptit.com.enghub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ptit.com.enghub.dto.ExerciseDTO;
import ptit.com.enghub.dto.LessonSeedData;
import ptit.com.enghub.dto.request.CompleteLessonRequest;
import ptit.com.enghub.dto.request.LessonCreationRequest;
import ptit.com.enghub.dto.response.ApiResponse;
import ptit.com.enghub.dto.response.LessonResponse;
import ptit.com.enghub.entity.Lesson;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.service.IService.ExerciseService;
import ptit.com.enghub.service.IService.LessonService;
import ptit.com.enghub.service.NotificationService;
import ptit.com.enghub.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final ExerciseService exerciseService;

    @GetMapping("/lessons/{id}")
    public ApiResponse<LessonResponse> getLesson(@PathVariable Long id) {
        return ApiResponse.<LessonResponse>builder()
                .result(lessonService.getLesson(id))
                .build();
    }

    @GetMapping("/units/{unitId}/lessons")
    public ApiResponse<List<LessonResponse>> getLessonsByUnitId(@PathVariable Long unitId) {
        return ApiResponse.<List<LessonResponse>>builder()
                .result(lessonService.getLessonsByUnitId(unitId))
                .build();
    }

    @PostMapping("/lessons/{id}/complete")
    public ApiResponse<Void> completeLesson(@PathVariable Long id, @RequestBody CompleteLessonRequest request) {
        lessonService.completeLesson(id, request);
        return ApiResponse.<Void>builder()
                .message("Lesson completed successfully")
                .build();
    }

    @GetMapping("/lessons/{lessonId}/exercises")
    public ApiResponse<List<ExerciseDTO>> getExercisesByLessonId(@PathVariable Long lessonId) {
        return ApiResponse.<List<ExerciseDTO>>builder()
                .result(exerciseService.getExercisesByLessonId(lessonId))
                .build();
    }

    @PostMapping("/lessons")
    public ApiResponse<Void> createLesson(@RequestBody LessonCreationRequest request) {
        Lesson lesson = lessonService.createLesson(request);
        return ApiResponse.<Void>builder()
                .message("Lesson created success")
                .build();
    }

    @DeleteMapping("/lessons/{id}")
    public ApiResponse<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ApiResponse.<Void>builder()
                .message("Lesson deleted successfully")
                .build();
    }


    @PutMapping("/lessons/{id}")
    public ApiResponse<LessonResponse> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonCreationRequest request
    ) {
        return ApiResponse.<LessonResponse>builder()
                .result(lessonService.updateLesson(id, request))
                .message("Lesson updated successfully")
                .build();
    }

    @PostMapping("/{lessonId}/seed")
    public ResponseEntity<Void> seedLesson(
            @PathVariable Long lessonId,
            @RequestBody LessonSeedData request
    ) {
        lessonService.seedLesson(lessonId, request);
        return ResponseEntity.ok().build();
    }

}
