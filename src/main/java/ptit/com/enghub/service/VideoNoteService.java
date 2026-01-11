package ptit.com.enghub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ptit.com.enghub.dto.VideoNotesDTO;
import ptit.com.enghub.entity.User;
import ptit.com.enghub.entity.VideoNote;
import ptit.com.enghub.repository.VideoNoteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoNoteService {

    private final VideoNoteRepository videoNoteRepository;
    private final UserService userService;

    public VideoNote createNote(VideoNotesDTO request) {
        User user = userService.getCurrentUser();
        VideoNote note = VideoNote.builder()
                .videoId(request.getVideoId())
                .userId(user.getId())
                .timestamp(request.getTimestamp())
                .content(request.getContent())
                .build();

        return videoNoteRepository.save(note);
    }

    public List<VideoNotesDTO> getNotes(Long videoId) {
        User user = userService.getCurrentUser();
        return videoNoteRepository
                .findByVideoIdAndUserIdOrderByTimestampAsc(videoId, user.getId())
                .stream()
                .map(note -> VideoNotesDTO.builder()
                        .id(note.getId())
                        .videoId(note.getVideoId())
                        .timestamp(note.getTimestamp())
                        .content(note.getContent())
                        .build()
                )
                .toList();
    }

    public void deleteNote(Long noteId) {
        User user = userService.getCurrentUser();
        VideoNote note = videoNoteRepository
                .findByIdAndUserId(noteId, user.getId())
                .orElseThrow(() -> new RuntimeException("Note không tồn tại"));

        videoNoteRepository.delete(note);
    }
}